/*
 * ============LICENSE_START=======================================================
 *
 * ================================================================================
 * Copyright (C) 2018 Nokia Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.controlloop.actor.appclcm;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class AppcLcmRecipeFormatterTest {

    @Test
    public void shouldReturnCapitalizedSingleWordRecipe() {
        //given
        AppcLcmRecipeFormatter recipeFormatter = new AppcLcmRecipeFormatter("moDify");
        String expectedRecipe = "Modify";

        //when
        String actualRecipe = recipeFormatter.getBodyRecipe();

        //then
        assertEquals(expectedRecipe, actualRecipe);
    }

    @Test
    public void shouldReturnCapitalizeAndJoinedMultiWordRecipe() {
        //given
        AppcLcmRecipeFormatter recipeFormatter = new AppcLcmRecipeFormatter("coNfig-moDify");
        String expectedRecipe = "ConfigModify";

        //when
        String actualRecipe = recipeFormatter.getBodyRecipe();

        //then
        assertEquals(expectedRecipe, actualRecipe);
    }

    @Test
    public void shouldReturnLowercasedSingleWordRecipe() {
        //given
        AppcLcmRecipeFormatter recipeFormatter = new AppcLcmRecipeFormatter("ModIfy");
        String expectedRecipe = "modify";

        //when
        String actualRecipe = recipeFormatter.getURLRecipe();

        //then
        assertEquals(expectedRecipe, actualRecipe);
    }

    @Test
    public void shouldReturnLowercasedDashJoinedMultiWordRecipe() {
        //given
        AppcLcmRecipeFormatter recipeFormatter = new AppcLcmRecipeFormatter("Config-MoDify");
        String expectedRecipe = "config-modify";

        //when
        String actualRecipe = recipeFormatter.getURLRecipe();

        //then
        assertEquals(expectedRecipe, actualRecipe);
    }
}