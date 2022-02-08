package model.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import model.basicDataTypes.WithAccess;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import utils.Deserializer;
import utils.Serializer;
import vocabularies.Vocabulary;

import java.util.Date;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Entity("ThesaurusAdmin")
public class ThesaurusAdmin {

    @Id
    @JsonSerialize(using = Serializer.ObjectIdSerializer.class)
    private ObjectId dbId;
    private WithAccess access;

    @JsonSerialize(using = Serializer.ObjectIdSerializer.class)
    private ObjectId creator;

    private String name;
    private String version;
    private String label;
    private Vocabulary.VocabularyType type;

    @JsonSerialize(using = Serializer.DateSerializer.class)
    @JsonDeserialize(using = Deserializer.DateDeserializer.class)
    private Date created;

    @JsonSerialize(using = Serializer.DateSerializer.class)
    @JsonDeserialize(using = Deserializer.DateDeserializer.class)
    private Date lastModified;

    public ThesaurusAdmin() {}

    public ThesaurusAdmin(String name, String version, String label, Vocabulary.VocabularyType type, ObjectId creatorId) {
        this.name = name;
        this.version = version;
        this.creator = creatorId;
        this.created = new Date();
        this.lastModified = new Date();
        this.label = label;
        this.type = type;

        WithAccess acs = new WithAccess();
        acs.setIsPublic(false);
        acs.addToAcl(new WithAccess.AccessEntry(creatorId, WithAccess.Access.OWN));
        this.access = acs;

    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Vocabulary.VocabularyType getType() {
        return type;
    }

    public void setType(Vocabulary.VocabularyType type) {
        this.type = type;
    }

    public ObjectId getDbId() {
        return dbId;
    }

    public WithAccess getAccess() {
        return access;
    }

    public void setAccess(WithAccess access) {
        this.access = access;
    }

    public ObjectId getCreator() {
        return creator;
    }

    public void setCreator(ObjectId creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
