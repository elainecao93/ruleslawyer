import json
from simple_io import write
from simple_io import clear
from contract.cards import Card

FORMATS = ["standard", "brawl", "historic", "pioneer", "modern", "legacy", "vintage", "commander", "pauper"]


def parse_card_oracle(card_json):
    if "oracle_text" in card_json:
        oracle = card_json["oracle_text"]
    else:
        oracle = parse_card_oracle(card_json["card_faces"][0]) + "\n//\n" + parse_card_oracle(card_json["card_faces"][1])
    if "power" in card_json:
        oracle = oracle + "\n" + str(card_json["power"]) + "/" + str(card_json["toughness"])
    if "loyalty" in card_json:
        oracle = oracle + "\nStarting Loyalty: " + str(card_json["loyalty"])
    return oracle


def main():
    all_cards = {}

    f = open("default-cards.json", "r", encoding="utf-8")
    text = f.read()
    print(type(text))
    raw_data = json.loads(text)
    print(type(raw_data))
    print(type(raw_data[0]))

    for (count, card_json) in enumerate(raw_data):
        if not card_json["lang"] == "en" or card_json["set_type"] in ["token", "memorabilia"]:
            continue

        card_name = card_json["name"].replace('"', "'")
        card_set = card_json["set_name"]
        if not card_json["prices"]["usd"]:
            continue  # means its not a paper product
        card_price = float(card_json["prices"]["usd"])
        print(str(count) + " " + card_name)

        if all_cards.get(card_name):
            all_cards[card_name].add_set(card_set, card_price)
        else:
            if "mana_cost" in card_json:
                mana_cost = card_json["mana_cost"]
            else:
                mana_cost = card_json["card_faces"][0]["mana_cost"]
            type_line = card_json["type_line"].replace("�", "-")
            oracle = parse_card_oracle(card_json).replace('"', "'")
            rulings = get_rulings(card_json["rulings_uri"])
            legalities = [k for k in card_json["legalities"] if card_json["legalities"][k] == "legal" and k in FORMATS]

            card = Card(card_name, mana_cost, type_line, oracle, rulings, card_set, legalities, card_price)
            all_cards[card_name] = card

        print(card)

    output = [all_cards[k] for k in all_cards]

    clear("cards.json")
    write("cards.json", output)


if __name__ == "__main__":
    main()
