package com.tadeasfort.threadsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class WebhookRequest {

    private String object;

    @JsonProperty("entry")
    private List<WebhookEntry> entries;

    // Constructors
    public WebhookRequest() {
    }

    public WebhookRequest(String object, List<WebhookEntry> entries) {
        this.object = object;
        this.entries = entries;
    }

    // Getters and setters
    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<WebhookEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<WebhookEntry> entries) {
        this.entries = entries;
    }

    // Inner class for webhook entry
    public static class WebhookEntry {
        private String id;
        private long time;
        private List<WebhookChange> changes;

        // Constructors
        public WebhookEntry() {
        }

        public WebhookEntry(String id, long time, List<WebhookChange> changes) {
            this.id = id;
            this.time = time;
            this.changes = changes;
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public List<WebhookChange> getChanges() {
            return changes;
        }

        public void setChanges(List<WebhookChange> changes) {
            this.changes = changes;
        }
    }

    // Inner class for webhook change
    public static class WebhookChange {
        private String field;
        private Map<String, Object> value;

        // Constructors
        public WebhookChange() {
        }

        public WebhookChange(String field, Map<String, Object> value) {
            this.field = field;
            this.value = value;
        }

        // Getters and setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Map<String, Object> getValue() {
            return value;
        }

        public void setValue(Map<String, Object> value) {
            this.value = value;
        }
    }
}