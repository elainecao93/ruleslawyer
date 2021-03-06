package search;

import chat_platform.HelpMessageService;
import chat_platform.rule_output.OutputFieldSplitService;
import contract.rules.*;
import contract.rules.enums.RuleRequestCategory;
import contract.rules.enums.RuleSource;
import contract.searchResults.RawRuleSearchResult;
import contract.searchResults.SearchResult;
import init_utils.ManaEmojiService;
import org.javacord.api.DiscordApi;
import search.contract.DiscordEmbedField;
import search.contract.DiscordSearchRequest;
import search.contract.DiscordSearchResult;
import search.contract.EmbedBuilderBuilder;
import search.contract.builder.DiscordSearchRequestBuilder;
import service.RawRuleSearchService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static contract.rules.enums.RuleRequestCategory.*;
import static contract.rules.enums.RuleSource.ANY_DOCUMENT;
import static ingestion.rule.JsonRuleIngestionService.getRawDigitalRulesData;
import static ingestion.rule.JsonRuleIngestionService.getRawRulesData;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static search.contract.builder.DiscordSearchRequestBuilder.aDiscordSearchRequest;

public class DiscordRuleSearchService {

    private HelpMessageService helpMessageService;
    private RawRuleSearchService rawRuleSearchService;
    private OutputFieldSplitService outputFieldSplitService;

    public static final Integer MAX_PAYLOAD_SIZE = 3000;
    public static final Integer MAX_FIELD_NAME_SIZE = 256;
    public static final Integer MAX_FIELD_VALUE_SIZE = 1024;
    public static final String NO_RESULTS_FOUND_HELP_MESSAGE = "RulesLawyer searches the following rules documents: the Comprehensive Rules, Infraction Procedure Guide, Magic Tournament Rules, Judging at Regular document, Oathbreaker rules, Digital Infraction Procedure Guide, and Digital Magic Tournament rules.\n" +
            "Much like a traditional search engine, if your query does not appear in these documents, no results will be returned. It is suggested that you only use words that you think are likely to appear in these documents.\n" +
            "For additional help, do {{help}} or {{help|advanced}}.";

    public DiscordRuleSearchService(DiscordApi api) {
        ManaEmojiService manaEmojiService = new ManaEmojiService(api);
        List<AbstractRule> rules = getRawRulesData().stream()
                .map(manaEmojiService::replaceManaSymbols)
                .collect(toList());
        List<AbstractRule> digitalRules = getRawDigitalRulesData().stream()
                .map(manaEmojiService::replaceManaSymbols)
                .collect(toList());
        this.helpMessageService = new HelpMessageService();
        this.rawRuleSearchService = new RawRuleSearchService(rules, digitalRules);
        this.outputFieldSplitService = new OutputFieldSplitService(MAX_FIELD_NAME_SIZE, MAX_FIELD_VALUE_SIZE);
    }

    public DiscordSearchResult getSearchResult(String author, String text) {
        String query = getQuery(text);
        if (query.equals(""))
            return null;

        if (query.startsWith("help")) {
            return query.equals("help") ?
                    new DiscordSearchResult(helpMessageService.getHelpFile()) :
                    new DiscordSearchResult(helpMessageService.getHelpFile(query.substring(5)));
        }

        DiscordSearchRequest discordSearchRequest = getSearchRequest(author, query);
        return getSearchResult(discordSearchRequest);
    }

    private String getQuery(String message) {
        int indexLeft = message.indexOf("{{");
        int indexRight = message.indexOf("}}");
        if (indexLeft == -1 || indexRight == -1 || indexRight < indexLeft)
            return "";
        return message.substring(indexLeft+2, indexRight).toLowerCase();
    }

    public DiscordSearchResult getSearchResult(DiscordSearchRequest discordSearchRequest) {
        RawRuleSearchResult rawResults = rawRuleSearchService.getRawResult(discordSearchRequest);

        if (rawResults.getRawResults().size() == 0) {
            return new DiscordSearchResult(
                    new EmbedBuilderBuilder()
                            .setAuthor("RulesLawyer")
                            .addFields(asList(
                                    new DiscordEmbedField(getEmbedTitle(discordSearchRequest), "No Results Found"),
                                    new DiscordEmbedField("Quick help", NO_RESULTS_FOUND_HELP_MESSAGE)
                            ))
                            .build()
            );
        }

        EmbedBuilderBuilder result = new EmbedBuilderBuilder()
                .setAuthor("RulesLawyer Rules Search")
                .setTitle(getEmbedTitle(discordSearchRequest));

        List<DiscordEmbedField> embedFields = rawResults.getRawResults().stream()
                .map(this::getFieldsForRawResult)
                .flatMap(Collection::stream)
                .collect(toList());
        List<List<DiscordEmbedField>> pages = splitResultPages(embedFields);

        Integer moddedPageNumber = (discordSearchRequest.getPageNumber() + pages.size()) % pages.size();
        result.addFields(pages.get(moddedPageNumber));

        String footer = getFooter(
                discordSearchRequest.getRequester(),
                moddedPageNumber+1,
                pages.size(),
                rawResults.getRuleRequestCategory(),
                rawResults.hasOtherCategory(),
                rawResults.isFuzzy()
        );

        result.setFooter(footer);

        return new DiscordSearchResult(result.build());

    }

    private String getFooter(
            String requester,
            Integer pageNumber,
            Integer pageSize,
            RuleRequestCategory ruleRequestCategory,
            boolean availableRuleSwap,
            boolean isFuzzy
    ) {
        String baseFooter;
        if (ruleRequestCategory == DIGITAL && availableRuleSwap) {
            baseFooter =  format("Requested by: %s | page %s of %s | paper rules available | paginate with arrow reactions",
                    requester, pageNumber, pageSize);
        } else if (ruleRequestCategory == PAPER && availableRuleSwap) {
            baseFooter =  format("Requested by: %s | page %s of %s | digital rules available | paginate with arrow reactions",
                    requester, pageNumber, pageSize);
        } else {
            baseFooter = format("Requested by: %s | page %s of %s | paginate with arrow reactions",
                    requester, pageNumber, pageSize);
        }
        return isFuzzy ?
                baseFooter + " | No exact match found. Automatically using experimental fuzzy search" :
                baseFooter;
    }

    private DiscordSearchRequest getSearchRequest(String author, String query) {
        DiscordSearchRequestBuilder ruleSearchRequest = aDiscordSearchRequest().setRequester(author);

        List<String> commands = asList(query.split("\\|"));

        commands.forEach(
                command -> {
                    if(command.startsWith("p") && command.length() < 5) {
                        try {
                            ruleSearchRequest.setPageNumber(parseInt(command.substring(1)));
                        } catch (NumberFormatException ignored) {
                            ruleSearchRequest.appendKeywords(getKeywordsFromSubquery(command));
                        }
                    }
                    else {
                        try {
                            ruleSearchRequest.setRuleSource(RuleSource.valueOf(command.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                            try {
                                ruleSearchRequest.setRuleRequestCategory(RuleRequestCategory.valueOf(command.toUpperCase()));
                            } catch (IllegalArgumentException alsoIgnored) {
                                ruleSearchRequest.appendKeywords(getKeywordsFromSubquery(command));
                            }
                        }
                    }
                }
        );

        return ruleSearchRequest.build();
    }

    private List<String> getKeywordsFromSubquery(String subquery) {
        if (subquery.startsWith("\"") && subquery.endsWith("\"")) {
            return singletonList(subquery.substring(1, subquery.length()-1).toLowerCase());
        }
        else {
            return stream(subquery.split(" "))
                    .map(String::toLowerCase)
                    .collect(toList());
        }
    }

    private String getEmbedTitle(DiscordSearchRequest discordSearchRequest) {
        String output = join("/", discordSearchRequest.getKeywords());
        if (discordSearchRequest.getRuleSource() != ANY_DOCUMENT) {
            output = discordSearchRequest.getRuleSource() + " | " + output;
        }
        if (discordSearchRequest.getRuleRequestCategory() != ANY_RULE_TYPE) {
            output = discordSearchRequest.getRuleRequestCategory().toString().toLowerCase() + " | " + output;
        }
        return output;
    }

    private List<DiscordEmbedField> getFieldsForRawResult(SearchResult<AbstractRule> result) {
        AbstractRule rule = result.getEntry();
        return rule.getPrintedRules().stream()
                .map(this::makeEmbedFieldsForRawText)
                .flatMap(Collection::stream)
                .peek(field -> field.setRelevancy(result.getRelevancy()))
                .collect(toList());
    }

    private List<DiscordEmbedField> makeEmbedFieldsForRawText(PrintableRule rule) {
        return outputFieldSplitService.getGenericRuleBlocks(rule).stream()
                .map(genericField -> new DiscordEmbedField(genericField.getHeader(), genericField.getBody()))
                .collect(toList());
    }

    private List<List<DiscordEmbedField>> splitResultPages(List<DiscordEmbedField> embedFields) {
        List<List<DiscordEmbedField>> pages = new ArrayList<>();

        List<DiscordEmbedField> currentPage = new ArrayList<>();

        for (int i=0; i<embedFields.size(); i++) {
            DiscordEmbedField field = embedFields.get(i);
            if (currentPage.stream().mapToInt(DiscordEmbedField::getLength).sum() + field.getLength() > MAX_PAYLOAD_SIZE ||
                    (i > 0 && field.getRelevancy()-embedFields.get(i-1).getRelevancy() > 3000)) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                currentPage.add(field);
            } else {
                currentPage.add(field);
            }
        }

        pages.add(currentPage);

        return pages;
    }
}
