package search.contract;

import exception.NotYetImplementedException;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.ArrayList;
import java.util.List;

//blame javacord for the shitty naming of this class
public class EmbedBuilderBuilder {

    private String author;
    private String title;
    private List<DiscordEmbedField> fields;
    private String footer;

    public EmbedBuilderBuilder() {
        fields = new ArrayList<>();
        this.author = "";
        this.title = "";
        this.footer = "";
    }

    public EmbedBuilderBuilder setAuthor(String author) {
        this.author = author;
        return this;
    }

    public EmbedBuilderBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public EmbedBuilderBuilder setFooter(String footer) {
        this.footer = footer;
        return this;
    }

    public EmbedBuilderBuilder addFields(List<DiscordEmbedField> fields) {
        this.fields.addAll(fields);
        return this;
    }

    public EmbedBuilderBuilder removeLastField() {
        if (fields.size() == 0)
            throw new NotYetImplementedException();
        fields.remove(fields.size()-1);
        return this;
    }

    public Integer getLength() {
        return title.length() + author.length() + footer.length()
                + fields.stream()
                .mapToInt(field -> field.getFieldName().length() + field.getFieldText().length())
                .sum();
    }

    public EmbedBuilder build() {
        EmbedBuilder output = new EmbedBuilder()
                .setTitle(title)
                .setAuthor(author)
                .setFooter(footer);
        fields.forEach(field -> output.addField(field.getFieldName(), field.getFieldText()));
        return output;
    }
}
