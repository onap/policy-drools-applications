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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class AppcLcmRecipeFormatter {

    private static final String SPLIT_SIGN = "-";
    private static final String BODY_SEPARATOR = "";
    private final List<String> splittedRecipe;

    AppcLcmRecipeFormatter(String rawRecipe) {
        splittedRecipe = Lists.newArrayList(rawRecipe.split(SPLIT_SIGN));
    }

    String getUrlRecipe() {
        return StringUtils.join(lowercaseStrings(splittedRecipe), SPLIT_SIGN);
    }

    String getBodyRecipe() {
        return StringUtils.join(capitalizeStrings(splittedRecipe), BODY_SEPARATOR);
    }

    private List<String> lowercaseStrings(List<String> splitedRecipe) {
        return formatString(splitedRecipe, StringUtils::lowerCase);
    }

    private List<String> capitalizeStrings(List<String> splitedRecipe) {
        return formatString(splitedRecipe, e -> StringUtils.capitalize(StringUtils.lowerCase(e)));
    }

    private List<String> formatString(List<String> splitedRecipe, Function<String, String> formatter) {
        return splitedRecipe
                .stream()
                .map(formatter)
                .collect(Collectors.toList());
    }
}
