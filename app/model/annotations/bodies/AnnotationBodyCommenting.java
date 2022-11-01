package model.annotations.bodies;

import model.basicDataTypes.MultiLiteral;

public class AnnotationBodyCommenting extends AnnotationBody {

    /**
     * The pref label of the URI tag, that will be used for display purposes.
     */
    private MultiLiteral label;

    public MultiLiteral getLabel() {
        return label;
    }

    public void setLabel(MultiLiteral label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "AnnotationBodyCommenting{" +
                "label=" + label +
                '}';
    }
}
