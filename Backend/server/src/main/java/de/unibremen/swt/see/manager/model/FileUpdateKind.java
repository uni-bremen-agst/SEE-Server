package de.unibremen.swt.see.manager.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FileUpdateKind {

    @JsonProperty("Updated")
    UPDATED,
    @JsonProperty("Renamed")
    RENAMED,
    @JsonProperty("Deleted")
    DELETED,
}