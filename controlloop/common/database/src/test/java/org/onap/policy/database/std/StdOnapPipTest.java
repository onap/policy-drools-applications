package org.onap.policy.database.std;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Status;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.database.ToscaDictionary;

public class StdOnapPipTest {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String A_VALUE = "a-value";
    private static final String ISSUER = ToscaDictionary.GUARD_ISSUER_PREFIX + "-my-issuer";

    private MyPip pip;
    private PIPRequest req;
    private PIPFinder finder;
    private StdMutablePIPResponse resp;

    /**
     * Initializes mocks and populates {@link #pip}.
     */
    @Before
    public void setUp() {
        req = mock(PIPRequest.class);
        finder = mock(PIPFinder.class);
        resp = new StdMutablePIPResponse();

        when(req.getIssuer()).thenReturn(ISSUER);

        pip = new MyPip();
    }

    @Test
    public void testAttributesProvided() {
        assertTrue(pip.attributesProvided().isEmpty());
    }

    @Test
    public void testIsRequestInvalid() {
        // valid issuer
        when(req.getIssuer()).thenReturn(ISSUER);
        assertFalse(pip.isRequestInvalid(req));

        // invalid issuer
        when(req.getIssuer()).thenReturn("bogus-issuer");
        assertTrue(pip.isRequestInvalid(req));

        // null issuer
        when(req.getIssuer()).thenReturn(null);
        assertTrue(pip.isRequestInvalid(req));
    }

    @Test
    public void testGetActor() {
        testGetArbitraryAttribute(StdOnapPip.PIP_REQUEST_ACTOR, pip2 -> pip2.getActor(finder));
    }

    @Test
    public void testGetRecipe() {
        testGetArbitraryAttribute(StdOnapPip.PIP_REQUEST_RECIPE, pip2 -> pip2.getRecipe(finder));
    }

    @Test
    public void testGetTarget() {
        testGetArbitraryAttribute(StdOnapPip.PIP_REQUEST_TARGET, pip2 -> pip2.getTarget(finder));
    }

    private void testGetArbitraryAttribute(PIPRequest request, Function<StdOnapPip, String> getter) {
        // target found
        pip = new MyPip() {
            @Override
            protected PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
                return resp;
            }

            @Override
            protected String findFirstAttributeValue(PIPResponse pipResponse) {
                return A_VALUE;
            }
        };

        pip = spy(pip);

        assertEquals(A_VALUE, getter.apply(pip));
        verify(pip).getAttribute(request, finder);
        verify(pip).findFirstAttributeValue(resp);


        // not found
        pip = new MyPip() {
            @Override
            protected PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
                return null;
            }

            @Override
            protected String findFirstAttributeValue(PIPResponse pipResponse) {
                return A_VALUE;
            }
        };

        pip = spy(pip);

        assertNull(getter.apply(pip));
        verify(pip).getAttribute(request, finder);
        verify(pip, never()).findFirstAttributeValue(resp);
    }

    @Test
    public void testGetAttribute() throws PIPException {
        when(finder.getMatchingAttributes(req, pip)).thenReturn(resp);

        Status status = mock(Status.class);
        Identifier ident = mock(Identifier.class);

        when(ident.stringValue()).thenReturn("my-attr-id");
        when(req.getAttributeId()).thenReturn(ident);

        // status != OK
        resp.setStatus(status);
        when(status.isOk()).thenReturn(false);
        assertNull(pip.getAttribute(req, finder));

        // status OK, empty attributes
        resp.setStatus(status);
        when(status.isOk()).thenReturn(true);
        assertNull(pip.getAttribute(req, finder));

        // status OK, has attributes
        resp.setStatus(status);
        when(status.isOk()).thenReturn(true);
        resp.setAttributes(Arrays.asList(mock(Attribute.class)));
        assertSame(resp, pip.getAttribute(req, finder));

        // null status, has attributes
        resp.setStatus(null);
        resp.setAttributes(Arrays.asList(mock(Attribute.class)));
        assertSame(resp, pip.getAttribute(req, finder));

        // with exception
        when(finder.getMatchingAttributes(req, pip)).thenThrow(new PIPException());
        assertNull(pip.getAttribute(req, finder));
    }

    @Test
    public void testFindFirstAttributeValue() {

        // no attributes
        resp.setAttributes(Collections.emptyList());
        assertNull(pip.findFirstAttributeValue(resp));

        // attribute that returns null
        Attribute attr = mock(Attribute.class);
        resp.setAttributes(Arrays.asList(attr, attr));
        assertNull(pip.findFirstAttributeValue(resp));

        // attribute that returns a list of null values
        Attribute attr2 = mock(Attribute.class);
        resp.setAttributes(Arrays.asList(attr, attr2));
        List<AttributeValue<String>> lst = Arrays.asList(makeAttr(null), makeAttr(null));
        when(attr.findValues(DataTypes.DT_STRING)).thenReturn(lst.iterator());
        assertNull(pip.findFirstAttributeValue(resp));

        // non-null value in the middle of the list
        lst = Arrays.asList(makeAttr(null), makeAttr(A_VALUE), makeAttr(null));
        when(attr.findValues(DataTypes.DT_STRING)).thenReturn(lst.iterator());
        assertEquals(A_VALUE, pip.findFirstAttributeValue(resp));
    }

    private AttributeValue<String> makeAttr(String value) {
        @SuppressWarnings("unchecked")
        AttributeValue<String> attrval = mock(AttributeValue.class);

        when(attrval.getValue()).thenReturn(value);

        return attrval;
    }

    @Test
    public void testAddIntegerAttribute() {
        resp = spy(resp);

        Identifier cat = mock(Identifier.class);
        Identifier attrid = mock(Identifier.class);

        pip.addIntegerAttribute(resp, cat, attrid, 100, req);

        verify(resp).addAttribute(any());

        // try with exception
        doThrow(new RuntimeException(EXPECTED_EXCEPTION)).when(resp).addAttribute(any());
        pip.addIntegerAttribute(resp, cat, attrid, 100, req);
    }

    @Test
    public void testAddStringAttribute() {
        resp = spy(resp);

        Identifier cat = mock(Identifier.class);
        Identifier attrid = mock(Identifier.class);

        pip.addStringAttribute(resp, cat, attrid, A_VALUE, req);

        verify(resp).addAttribute(any());

        // try with exception
        doThrow(new RuntimeException(EXPECTED_EXCEPTION)).when(resp).addAttribute(any());
        pip.addStringAttribute(resp, cat, attrid, A_VALUE, req);
    }

    private class MyPip extends StdOnapPip {

        @Override
        public Collection<PIPRequest> attributesRequired() {
            return Collections.emptyList();
        }

        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
            return null;
        }

    }
}
