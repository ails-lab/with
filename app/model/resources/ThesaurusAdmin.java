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
    private String name;
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

}
