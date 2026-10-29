package com.reelbite.app.domain

/**
 * A recognisable dish.
 *
 * @param template group name; `{p}` is replaced by the detected main ingredient
 *                 ("{p} Biryani" -> "Chicken Biryani"). Templates without `{p}` are fixed names.
 * @param defaultName name used when the template needs an ingredient but none was found.
 * @param tier 2 = a "main dish" word (biryani, pizza…), 1 = a cooking style (fry, curry…).
 *             Main dishes win close calls, so "chicken fry piece biryani" is a biryani.
 */
data class Dish(
    val id: String,
    val template: String,
    val defaultName: String,
    val emoji: String,
    val aliases: List<String>,
    val tier: Int = 2,
) {
    val takesIngredient: Boolean get() = "{p}" in template
}

/** Main ingredient / protein used to qualify a dish (Chicken, Mutton, Paneer…). */
data class Ingredient(
    val id: String,
    val display: String,
    val emoji: String,
    val aliases: List<String>,
)

object DishCatalog {

    /**
     * Phrases that contain dish/ingredient words but don't describe the dish
     * ("garam masala", "fry the onions", "eggless"). They are consumed before matching.
     */
    val ignorePhrases: List<String> = listOf(
        "garam masala", "chaat masala", "chat masala", "biryani masala", "biriyani masala",
        "chicken masala powder", "meat masala", "masala powder", "kitchen king masala",
        "curry leaves", "curry leaf", "curry powder",
        "air fryer", "airfryer", "air fried", "air fry",
        "fried onions", "fried onion", "fry onions", "fry onion", "fry the onions", "fry the onion",
        "fry till", "fry until", "fry them", "fry it", "fry for",
        "bread crumbs", "breadcrumbs", "bread crumb",
        "non veg", "nonveg", "eggless", "without egg", "no egg",
        "teaspoon", "teaspoons", "tea spoon", "tea spoons",
        "soy sauce", "soya sauce", "chicken stock", "chicken broth",
    )

    val dishes: List<Dish> = listOf(
        // ---- Fixed-name classics (ingredient is part of the name) ----
        Dish("butter_chicken", "Butter Chicken", "Butter Chicken", "🍛",
            listOf("butter chicken", "murgh makhani", "murg makhani", "chicken makhani", "makhani chicken")),
        Dish("palak_paneer", "Palak Paneer", "Palak Paneer", "🥬", listOf("palak paneer", "saag paneer")),
        Dish("pav_bhaji", "Pav Bhaji", "Pav Bhaji", "🍛", listOf("pav bhaji", "pao bhaji")),
        Dish("vada_pav", "Vada Pav", "Vada Pav", "🍔", listOf("vada pav", "wada pav", "vada pao")),
        Dish("misal", "Misal Pav", "Misal Pav", "🍲", listOf("misal pav", "misal")),
        Dish("chole_bhature", "Chole Bhature", "Chole Bhature", "🫓",
            listOf("chole bhature", "chole bhatura", "chhole bhature", "bhature", "bhatura")),
        Dish("chole", "Chole", "Chole", "🫘", listOf("chole", "chhole", "cholay", "chana masala", "chickpea curry")),
        Dish("rajma", "Rajma", "Rajma", "🫘", listOf("rajma", "rajma chawal", "kidney bean curry")),
        Dish("omelette", "Omelette", "Omelette", "🍳",
            listOf("omelette", "omelettes", "omelet", "omelets", "omlet", "masala omelette")),

        // ---- Rice ----
        Dish("biryani", "{p} Biryani", "Biryani", "🍛",
            listOf(
                "biryani", "biriyani", "biriani", "briyani", "bryani", "biryanii", "biryanis", "biriyanis",
                "dum biryani", "fry piece biryani", "बिरयानी", "బిర్యానీ", "பிரியாணி",
            )),
        Dish("mandi", "{p} Mandi", "Mandi", "🍛", listOf("mandi", "kabsa", "kuzhimandi")),
        Dish("pulao", "{p} Pulao", "Pulao", "🍚",
            listOf("pulao", "pulav", "pulaav", "pilaf", "pilau", "palav", "tahari", "tehri")),
        Dish("fried_rice", "{p} Fried Rice", "Fried Rice", "🍚", listOf("fried rice", "schezwan rice")),
        Dish("rice", "{p} Rice", "Rice Dishes", "🍚",
            listOf(
                "jeera rice", "lemon rice", "curd rice", "tomato rice", "ghee rice", "coconut rice",
                "tamarind rice", "pulihora", "puliyogare", "bisi bele bath", "bisibelebath", "khichdi",
                "khichuri", "rice bowl",
            )),
        Dish("haleem", "{p} Haleem", "Haleem", "🍲", listOf("haleem", "harees")),
        Dish("nihari", "{p} Nihari", "Nihari", "🍲", listOf("nihari", "nahari", "paya")),

        // ---- Indo-Chinese & starters ----
        Dish("noodles", "{p} Noodles", "Noodles", "🍜",
            listOf("noodles", "noodle", "chow mein", "chowmein", "hakka noodles", "ramen", "maggi", "pad thai", "lo mein")),
        Dish("manchurian", "{p} Manchurian", "Manchurian", "🥡", listOf("manchurian", "manchuria")),
        Dish("sixty_five", "{p} 65", "65 Fry", "🌶️",
            listOf(
                "chicken 65", "gobi 65", "paneer 65", "mushroom 65", "fish 65", "prawn 65", "prawns 65",
                "egg 65", "baby corn 65", "aloo 65", "soya 65",
            )),
        Dish("chilli", "Chilli {p}", "Chilli Fry", "🌶️",
            listOf(
                "chilli chicken", "chili chicken", "chilly chicken", "chilli paneer", "chili paneer",
                "chilly paneer", "chilli gobi", "chilli fish", "chilli prawn", "chilli prawns",
                "chilli mushroom", "chilli egg", "chilli potato", "honey chilli potato",
                "chilli baby corn", "chilli soya", "dragon chicken",
            )),
        Dish("lollipop", "{p} Lollipop", "Lollipop", "🍗", listOf("lollipop", "lollipops", "lollypop", "lolipop")),
        Dish("wings", "{p} Wings", "Wings", "🍗", listOf("wings", "wing")),
        Dish("nuggets", "{p} Nuggets", "Nuggets", "🍗", listOf("nugget", "nuggets", "popcorn chicken", "chicken popcorn")),
        Dish("spring_roll", "{p} Spring Roll", "Spring Rolls", "🥢", listOf("spring roll", "spring rolls")),
        Dish("momos", "{p} Momos", "Momos", "🥟",
            listOf("momo", "momos", "dumpling", "dumplings", "dimsum", "dim sum", "gyoza", "wonton", "wontons")),
        Dish("stir_fry", "{p} Stir Fry", "Stir Fry", "🥘", listOf("stir fry", "stir fried", "stir fries")),

        // ---- Tandoor / grill ----
        Dish("tandoori", "Tandoori {p}", "Tandoori", "🔥", listOf("tandoori", "tandoor")),
        Dish("tikka_masala", "{p} Tikka Masala", "Tikka Masala", "🍛", listOf("tikka masala")),
        Dish("butter_masala", "{p} Butter Masala", "Butter Masala", "🍛",
            listOf("butter masala", "makhani", "makhanwala", "makhanwalla")),
        Dish("tikka", "{p} Tikka", "Tikka", "🍢", listOf("tikka", "tikkas")),
        Dish("kebab", "{p} Kebab", "Kebabs", "🍢",
            listOf("kebab", "kebabs", "kabab", "kababs", "kabob", "kebap", "seekh", "shami", "galouti", "kabobs")),
        Dish("bbq", "{p} BBQ", "BBQ & Grills", "🍖", listOf("bbq", "barbecue", "barbeque", "grilled", "grill")),
        Dish("steak", "{p} Steak", "Steak", "🥩", listOf("steak", "steaks")),

        // ---- Gravies ----
        Dish("kadai", "Kadai {p}", "Kadai", "🍲", listOf("kadai", "kadhai", "karahi")),
        Dish("korma", "{p} Korma", "Korma", "🍲", listOf("korma", "kurma", "qorma")),
        Dish("ghee_roast", "{p} Ghee Roast", "Ghee Roast", "🔥", listOf("ghee roast")),
        Dish("pepper", "Pepper {p}", "Pepper Fry", "🌶️",
            listOf("pepper chicken", "pepper fry", "pepper mutton", "pepper prawns", "pepper fish", "milagu")),
        Dish("curry", "{p} Curry", "Curry", "🍲",
            listOf("curry", "curries", "kulambu", "kuzhambu", "kozhambu", "salan", "gravy", "rassa", "pulusu"),
            tier = 1),
        Dish("masala", "{p} Masala", "Masala", "🍲", listOf("masala"), tier = 1),
        Dish("fry", "{p} Fry", "Fry", "🍗",
            listOf("fry", "fried", "frys", "vepudu", "varuval", "sukka", "chukka", "dry fry", "pakodi fry"),
            tier = 1),
        Dish("roast", "{p} Roast", "Roast", "🍖", listOf("roast"), tier = 1),
        Dish("bhurji", "{p} Bhurji", "Bhurji", "🍳", listOf("bhurji", "burji", "scramble", "scrambled")),
        Dish("sabzi", "{p} Sabzi", "Sabzi", "🥘", listOf("sabzi", "sabji", "subzi", "poriyal", "thoran", "palya", "bhaji")),

        // ---- Snacks ----
        Dish("cutlet", "{p} Cutlet", "Cutlets", "🧆", listOf("cutlet", "cutlets", "patty", "patties", "croquette", "croquettes")),
        Dish("pakora", "{p} Pakora", "Pakoras & Fritters", "🧆",
            listOf(
                "pakora", "pakoras", "pakoda", "pakodas", "pakodi", "bajji", "bhajji", "bhajiya", "onion bhaji",
                "fritters", "fritter", "bonda", "bondas",
            )),
        Dish("samosa", "{p} Samosa", "Samosa", "🥟", listOf("samosa", "samosas")),
        Dish("kachori", "Kachori", "Kachori", "🥟", listOf("kachori", "kachoris", "kachodi")),
        Dish("vada", "{p} Vada", "Vada", "🍩", listOf("vada", "vadai", "vadas", "wada", "medu vada")),
        Dish("chaat", "Chaat", "Chaat", "🥗",
            listOf(
                "chaat", "pani puri", "panipuri", "golgappa", "gol gappa", "golgappe", "puchka", "phuchka",
                "bhel", "bhelpuri", "bhel puri", "sev puri", "dahi puri", "papdi chaat", "ragda",
            )),
        Dish("fries", "Fries", "Fries", "🍟", listOf("fries", "french fries", "finger chips", "potato wedges", "wedges")),

        // ---- Breakfast / breads ----
        Dish("dosa", "{p} Dosa", "Dosa", "🥞", listOf("dosa", "dosas", "dosai", "masala dosa")),
        Dish("idli", "Idli", "Idli", "🍚", listOf("idli", "idly", "idlis")),
        Dish("uttapam", "Uttapam", "Uttapam", "🥞", listOf("uttapam", "uthappam", "uttappa", "oothappam")),
        Dish("upma", "Upma", "Upma", "🥣", listOf("upma", "uppittu", "rava upma")),
        Dish("poha", "Poha", "Poha", "🥣", listOf("poha", "avalakki", "aval upma")),
        Dish("paratha", "{p} Paratha", "Paratha", "🫓",
            listOf("paratha", "parathas", "parotta", "porotta", "parata", "prantha", "parantha")),
        Dish("roti", "Roti & Chapati", "Roti & Chapati", "🫓",
            listOf("roti", "rotis", "chapati", "chapathi", "chapatti", "phulka", "fulka")),
        Dish("naan", "{p} Naan", "Naan", "🫓", listOf("naan", "naans", "kulcha", "kulchas")),
        Dish("puri", "Puri", "Puri", "🫓", listOf("puri", "puris", "poori", "pooris", "puri bhaji")),
        Dish("sandwich", "{p} Sandwich", "Sandwich", "🥪",
            listOf("sandwich", "sandwiches", "sandwhich", "toastie", "grilled cheese")),
        Dish("burger", "{p} Burger", "Burger", "🍔", listOf("burger", "burgers")),
        Dish("pizza", "{p} Pizza", "Pizza", "🍕", listOf("pizza", "pizzas")),
        Dish("pasta", "{p} Pasta", "Pasta", "🍝",
            listOf(
                "pasta", "spaghetti", "penne", "macaroni", "fettuccine", "lasagna", "lasagne",
                "mac and cheese", "mac n cheese", "alfredo", "carbonara",
            )),
        Dish("wrap", "{p} Wrap", "Wraps & Rolls", "🌯",
            listOf("wrap", "wraps", "kathi roll", "kati roll", "frankie", "burrito", "burritos", "quesadilla", "taco", "tacos")),
        Dish("shawarma", "{p} Shawarma", "Shawarma", "🌯", listOf("shawarma", "shawarama", "shwarma", "shawerma")),
        Dish("pancake", "Pancakes", "Pancakes", "🥞", listOf("pancake", "pancakes", "crepe", "crepes")),
        Dish("waffle", "Waffles", "Waffles", "🧇", listOf("waffle", "waffles")),
        Dish("french_toast", "French Toast", "French Toast", "🍞", listOf("french toast")),
        Dish("bread", "Breads", "Breads", "🍞",
            listOf("bread", "breads", "loaf", "focaccia", "sourdough", "buns", "garlic bread", "pav")),

        // ---- Lentils & sides ----
        Dish("dal", "Dal", "Dal", "🥣",
            listOf("dal", "daal", "dhal", "dhall", "dal tadka", "dal fry", "dal makhani", "pappu")),
        Dish("sambar", "Sambar", "Sambar", "🥣", listOf("sambar", "sambhar", "sambaar")),
        Dish("rasam", "Rasam", "Rasam", "🥣", listOf("rasam", "saaru", "chaaru")),
        Dish("soup", "{p} Soup", "Soup", "🍜", listOf("soup", "soups", "shorba", "manchow")),
        Dish("salad", "{p} Salad", "Salad", "🥗", listOf("salad", "salads")),
        Dish("chutney", "Chutney", "Chutney", "🫙", listOf("chutney", "chutneys", "pachadi", "thogayal", "thuvaiyal")),
        Dish("pickle", "{p} Pickle", "Pickles", "🫙", listOf("pickle", "pickles", "achar", "achaar", "avakaya", "avakai")),

        // ---- Desserts ----
        Dish("cheesecake", "Cheesecake", "Cheesecake", "🍰", listOf("cheesecake", "cheesecakes")),
        Dish("cake", "Cakes", "Cakes", "🍰", listOf("cake", "cakes", "cupcake", "cupcakes")),
        Dish("brownie", "Brownies", "Brownies", "🍫", listOf("brownie", "brownies", "blondie", "blondies")),
        Dish("cookies", "Cookies", "Cookies", "🍪", listOf("cookie", "cookies", "biscuit", "biscuits")),
        Dish("donut", "Donuts", "Donuts", "🍩", listOf("donut", "donuts", "doughnut", "doughnuts")),
        Dish("ice_cream", "Ice Cream", "Ice Cream", "🍨", listOf("ice cream", "icecream", "gelato", "kulfi", "sorbet", "sundae")),
        Dish("kheer", "Kheer & Payasam", "Kheer & Payasam", "🍮",
            listOf("kheer", "payasam", "payesh", "phirni", "firni", "sheer khurma")),
        Dish("halwa", "Halwa", "Halwa", "🍮", listOf("halwa", "halva", "sheera", "kesari")),
        Dish("indian_sweets", "Indian Sweets", "Indian Sweets", "🍬",
            listOf(
                "gulab jamun", "laddu", "ladoo", "laddoo", "jalebi", "barfi", "burfi", "rasgulla",
                "rasmalai", "ras malai", "peda", "mysore pak", "kaju katli",
            )),
        Dish("dessert", "Desserts", "Desserts", "🍮",
            listOf("pudding", "custard", "mousse", "tiramisu", "panna cotta", "trifle", "dessert", "desserts"),
            tier = 1),

        // ---- Drinks ----
        Dish("coffee", "Coffee", "Coffee", "☕",
            listOf("coffee", "latte", "cappuccino", "espresso", "dalgona", "cold coffee", "frappe")),
        Dish("tea", "Tea & Chai", "Tea & Chai", "🍵", listOf("chai", "tea", "masala chai", "matcha")),
        Dish("smoothie", "Smoothies & Shakes", "Smoothies & Shakes", "🥤",
            listOf("smoothie", "smoothies", "milkshake", "milkshakes", "shake", "shakes", "lassi")),
        Dish("drinks", "Juices & Drinks", "Juices & Drinks", "🍹",
            listOf("juice", "juices", "mocktail", "mocktails", "lemonade", "sharbat", "sherbet", "mojito", "nimbu pani")),
    )

    val ingredients: List<Ingredient> = listOf(
        Ingredient("chicken", "Chicken", "🍗",
            listOf(
                "chicken", "chickens", "chiken", "chikken", "murgh", "murg", "kodi", "koli", "kozhi",
                "country chicken", "natukodi", "nati koli", "चिकन", "చికెన్", "சிக்கன்",
            )),
        Ingredient("mutton", "Mutton", "🍖", listOf("mutton", "goat", "lamb", "gosht", "ghosht", "mamsam", "bakra")),
        Ingredient("beef", "Beef", "🥩", listOf("beef")),
        Ingredient("pork", "Pork", "🥓", listOf("pork", "bacon")),
        Ingredient("fish", "Fish", "🐟",
            listOf(
                "fish", "machli", "machhi", "meen curry", "meen fry", "meen varuval", "meen kuzhambu",
                "salmon", "tuna", "pomfret", "surmai", "rohu", "kingfish", "king fish", "basa", "tilapia",
                "sardine", "sardines", "mackerel", "bangda", "chepa",
            )),
        Ingredient("prawn", "Prawn", "🦐",
            listOf("prawn", "prawns", "shrimp", "shrimps", "jhinga", "jheenga", "royyalu", "chemmeen", "eral")),
        Ingredient("crab", "Crab", "🦀", listOf("crab", "crabs", "nandu", "peetha")),
        Ingredient("seafood", "Seafood", "🦑", listOf("squid", "calamari", "octopus", "lobster", "mussels", "clams", "seafood")),
        Ingredient("egg", "Egg", "🥚", listOf("egg", "eggs", "anda", "ande", "kodi guddu", "guddu", "muttai", "mutta")),
        Ingredient("paneer", "Paneer", "🧀", listOf("paneer", "panner", "cottage cheese")),
        Ingredient("mushroom", "Mushroom", "🍄", listOf("mushroom", "mushrooms")),
        Ingredient("veg", "Veg", "🥦",
            listOf(
                "veg", "veggie", "veggies", "vegetable", "vegetables", "vegetarian", "vegan",
                "mix veg", "mixed veg", "mixed vegetable",
            )),
        Ingredient("aloo", "Aloo", "🥔", listOf("aloo", "alu", "potato", "potatoes", "batata", "urulai")),
        Ingredient("gobi", "Gobi", "🥦", listOf("gobi", "gobhi", "cauliflower")),
        Ingredient("soya", "Soya", "🫘", listOf("soya", "soy", "meal maker", "soyabean", "soya chunks")),
        Ingredient("chana", "Chana", "🫘", listOf("chana", "chickpea", "chickpeas")),
        Ingredient("brinjal", "Brinjal", "🍆",
            listOf("brinjal", "baingan", "eggplant", "aubergine", "vankaya", "kathirikai", "badanekai")),
        Ingredient("bhindi", "Bhindi", "🫛", listOf("bhindi", "okra", "ladies finger", "lady finger", "bendakaya", "vendakkai")),
        Ingredient("corn", "Corn", "🌽", listOf("baby corn", "babycorn", "corn", "sweet corn", "sweetcorn", "bhutta")),
        Ingredient("tofu", "Tofu", "🫘", listOf("tofu")),
        Ingredient("palak", "Palak", "🥬", listOf("palak", "spinach", "saag")),
    )
}
