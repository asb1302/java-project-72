package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Lob;
import javax.persistence.Id;
import java.time.Instant;

@Entity
public final class UrlCheck extends Model {
    public UrlCheck(
            int statusCode,
            String title,
            String h1,
            String description,
            Url url
    ) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.url = url;
    }

    @Id
    private long id;

    private Integer statusCode;

    private String title;
    private String h1;
    @Lob
    private String description;

    @ManyToOne(optional = false)
    private Url url;

    @WhenCreated
    private Instant createdAt;

    public long getId() {
        return id;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getTitle() {
        return title;
    }

    public String getH1() {
        return h1;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
