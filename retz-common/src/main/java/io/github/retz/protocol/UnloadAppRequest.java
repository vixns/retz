/**
 *    Retz
 *    Copyright (C) 2016 Nautilus Technologies, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.retz.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kuenishi on 6/6/16.
 */
public class UnloadAppRequest extends Request {
    private String appid;

    @JsonCreator
    public UnloadAppRequest(@JsonProperty(value = "appid", required = true) String appName) {
        appid = appName;
    }

    @JsonGetter("appid")
    public String appid() {
        return appid;
    }

    @Override
    public String resource() {
        return "/app/" + appid;
    }

    @Override
    public String method() {
        return DELETE;
    }

    @Override
    public boolean hasPayload() {
        return false;
    }
    public static String resourcePattern() {
        return "/app/:name";
    }
}
