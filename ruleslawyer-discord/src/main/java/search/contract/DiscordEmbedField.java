package search.contract;

public class DiscordEmbedField {
    private String fieldName;
    private String fieldText;
    private Integer relevancy;

    public DiscordEmbedField(String fieldName, String fieldText) {
        this.fieldName = fieldName;
        this.fieldText = fieldText;
        if (fieldText.length() == 0)
            this.fieldText = ".";
        this.relevancy = 0;
    }

    public DiscordEmbedField(String fieldName, String fieldText, Integer relevancy) {
        this.fieldName = fieldName;
        this.fieldText = fieldText;
        if (fieldText.length() == 0)
            this.fieldText = ".";
        this.relevancy = relevancy;
    }

    public void setRelevancy(Integer relevancy) {
        this.relevancy = relevancy;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldText() {
        return fieldText;
    }

    public Integer getRelevancy() {
        return relevancy;
    }

    public Integer getLength() {
        return fieldName.length() + fieldText.length();
    }
}
