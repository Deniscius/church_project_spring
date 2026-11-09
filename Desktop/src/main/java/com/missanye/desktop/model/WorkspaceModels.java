package com.missanye.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Read models for server catalogues and local/server operation rows. */
public final class WorkspaceModels {
    private WorkspaceModels() {}
    public record Choice(String id,String label,JsonNode data){@Override public String toString(){return label;}}
    public record Row(JsonNode data,String label){@Override public String toString(){return label;}}
}
