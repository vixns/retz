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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(property = "command",
        use = Id.NAME,
        include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @Type(name = "list", value = ListJobRequest.class),
        @Type(name = "schedule", value = ScheduleRequest.class),
        @Type(name = "get-job", value = GetJobRequest.class),
        @Type(name = "get-file", value = GetFileRequest.class),
        @Type(name = "list-files", value = ListFilesRequest.class),
        @Type(name = "kill", value = KillRequest.class),
        @Type(name = "get-app", value = GetAppRequest.class),
        @Type(name = "load-app", value = LoadAppRequest.class),
        @Type(name = "unload-app", value = UnloadAppRequest.class),
        @Type(name = "list-app", value = ListAppRequest.class)
})
public abstract class Request {
    public static final String PUT = "PUT";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    public abstract String resource();
    public abstract String method();
    public abstract boolean hasPayload();
}
