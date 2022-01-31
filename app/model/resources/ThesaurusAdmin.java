package model.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import model.basicDataTypes.WithAccess;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;
import utils.Deserializer;
import utils.Serializer;

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

    private String vocabularyName;
    private String vocabularyVersion;

    @JsonSerialize(using = Serializer.DateSerializer.class)
    @JsonDeserialize(using = Deserializer.DateDeserializer.class)
    private Date created;

    @JsonSerialize(using = Serializer.DateSerializer.class)
    @JsonDeserialize(using = Deserializer.DateDeserializer.class)
    @Version
    private Date lastModified;

    public ThesaurusAdmin() {}

    public ThesaurusAdmin(String name, String version, ObjectId creatorId) {
        this.vocabularyName = name;
        this.vocabularyVersion = version;
        this.creator = creatorId;
        this.created = new Date();
        this.lastModified = new Date();

        WithAccess acs = new WithAccess();
        acs.setIsPublic(false);
        acs.addToAcl(new WithAccess.AccessEntry(creatorId, WithAccess.Access.OWN));
        this.access = acs;

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

    public String getVocabularyName() {
        return vocabularyName;
    }

    public void setVocabularyName(String vocabularyName) {
        this.vocabularyName = vocabularyName;
    }

    public String getVocabularyVersion() {
        return vocabularyVersion;
    }

    public void setVocabularyVersion(String vocabularyVersion) {
        this.vocabularyVersion = vocabularyVersion;
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
