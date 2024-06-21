package model.annotations.bodies;

public class AnnotationBodySubTagging extends AnnotationBodyTagging {

    /**
     * The description of the URI tag, that will be used for display purposes.
     */
    private MultiLiteral description;

    public MultiLiteral getDescription() {
        return description;
    }

    public void setDescription(MultiLiteral description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AnnotationBodySubTagging{" +
                "uri='" + uri + '\'' +
                ", tagType='" + tagType + '\'' +
                ", label=" + label +
                ", description=" + description +
                ", uriType=" + uriType +
                ", uriVocabulary='" + uriVocabulary + '\'' +
                '}';
    }
}
