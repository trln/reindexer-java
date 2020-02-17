package org.trln.discovery;

public class Document {
    public String getId() {
        return id;
    }

    public Document setId(String id) {
        this.id = id;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public Document setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Document setContent(String content) {
        this.content = content;
        return this;
    }

    String id;
    String owner;
    String content;
}
