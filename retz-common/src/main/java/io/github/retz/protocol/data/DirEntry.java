/**
 * Retz
 * Copyright (C) 2016 Nautilus Technologies, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.retz.protocol.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

// A JSON data structure defined at Mesos file API '/files/browse'
// http://mesos.apache.org/documentation/latest/endpoints/files/browse/
public class DirEntry {
    private String gid;
    private String mode;
    private int mtime;
    private int nlink;
    private String path;
    private int size;
    private String uid;

    @JsonCreator
    public DirEntry(@JsonProperty("gid") String gid,
                    @JsonProperty("mode") String mode,
                    @JsonProperty("mtime") int mtime,
                    @JsonProperty("nlink") int nlink,
                    @JsonProperty("path") String path,
                    @JsonProperty("size") int size,
                    @JsonProperty("uid") String uid) {
        this.gid = gid;
        this.mode = mode;
        this.mtime = mtime;
        this.nlink = nlink;
        this.path = path;
        this.size = size;
        this.uid = uid;
    }

    @JsonGetter("gid")
    public String gid() {
        return gid;
    }
    @JsonGetter("mode")
    public String mode() {
        return mode;
    }
    @JsonGetter("mtime")
    public int mtime() {
        return mtime;
    }
    @JsonGetter("nlink")
    public int nlink() {
        return nlink;
    }
    @JsonGetter("path")
    public String path() {
        return path;
    }
    @JsonGetter("size")
    public int size() {
        return size;
    }
    @JsonGetter("uid")
    public String uid() {
        return uid;
    }

}
