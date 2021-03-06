import contract.searchResults.SearchResult;
import contract.cards.Card;
import contract.searchRequests.CardSearchRequest;
import ingestion.card.JsonCardIngestionService;
import repository.SearchRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static contract.cards.FormatLegality.ANY_FORMAT;
import static contract.searchRequests.CardSearchRequestType.DEFAULT;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

public class CardSearchIntegrationCLTest {

    public static void main(String[] args) throws IOException {
        List<Card> searchSpace = JsonCardIngestionService.getCards();
        SearchRepository<Card> repository = new SearchRepository<>(searchSpace);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String input = reader.readLine();
            CardSearchRequest request = new CardSearchRequest(asList(input.split(" ")), DEFAULT, ANY_FORMAT);
            List<SearchResult<Card>> results = repository.getSearchResult(request);
            String output = results.stream()
                    .map(SearchResult::getEntry)
                    .map(Card::getCardName)
                    .limit(20)
                    .collect(joining("\n"));
            System.out.println(output);
        }
    }
}
