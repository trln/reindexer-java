package org.trln.discovery;

public class Config {

    private int chunkSize = 20000;
    private String host;
    private String name;
    private String query = "select id, txn_id, owner, content from documents WHERE NOT deleted";
    private String user;
    private String password;
    private String solrUrl;
    private int workers = Runtime.getRuntime().availableProcessors() -1;
    /*
    {
        "host": "{{ database.host }}",
            "name" : "{{ database.name }}",
            "user" : "{{ database.user }}",
            "password": "{{ database.password | replace('\\', '') }}",
            "solrUrl" : "{{ solr_url }}"
    }
    */

    public int getChunkSize() {
        return chunkSize;
    }

    public Config setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public Config setQuery(String query) {
        this.query = query;
        return this;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public String getUser() {
        return user;
    }

    public Config setUser(String user) {
        this.user = user;
        return this;
    }


    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
    }

    public void validate() throws IllegalStateException {
        boolean valid = true;
        if ( workers < 1 ) {
            throw new IllegalStateException("workers must be at least 1 found:(" + workers + ")");
        }
        if ( host == null ) {
            throw new IllegalStateException("host cannot be null");

        }
    }
}
