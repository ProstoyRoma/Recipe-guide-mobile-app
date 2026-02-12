# tfidf_android.py
import os
import json
import joblib
import re
import ast
import unidecode
import string
import numpy as np
from collections import defaultdict

# Убираем gensim import — используем SimpleWordLookup вместо Word2Vec
# from gensim.models import Word2Vec

# nltk (если нужен)
from nltk.stem import WordNetLemmatizer
import nltk
from nltk.corpus import stopwords

# --- ensure nltk data (опционально) ---
try:
    stopwords.words("english")
    nltk.data.find("corpora/wordnet")
except LookupError:
    nltk.download("stopwords", quiet=True)
    nltk.download("wordnet", quiet=True)
    nltk.download("omw-1.4", quiet=True)

# ------------------ вспомогательные функции (ваш код) ------------------
_UNICODE_REPLACEMENTS = {
    "\u2018": "'", "\u2019": "'", "\u201c": '"', "\u201d": '"',
    "\u2013": "-", "\u2014": "-", "½": "1/2", "¼": "1/4", "¾": "3/4",
    "⅓": "1/3", "⅔": "2/3", "⅛": "1/8"
}

def _normalize_unicode(s: str) -> str:
    if not isinstance(s, str):
        return s
    for k, v in _UNICODE_REPLACEMENTS.items():
        s = s.replace(k, v)
    s = re.sub(r"[\u200b\u200c\u200d\uFEFF]", "", s)
    return s

def safe_parse_ingredient_list(value):
    if value is None:
        return []
    if isinstance(value, list):
        return [str(v) for v in value if v is not None]
    s = str(value).strip()
    if s == "":
        return []
    s = _normalize_unicode(s)
    try:
        if s[0] in ("[", "("):
            parsed = ast.literal_eval(s)
            if isinstance(parsed, list):
                return [str(x) for x in parsed]
    except Exception:
        pass
    try:
        s_json = s
        if s_json.startswith("[") and "'" in s_json and '"' not in s_json:
            s_json = s_json.replace("'", '"')
        parsed = json.loads(s_json)
        if isinstance(parsed, list):
            return [str(x) for x in parsed]
    except Exception:
        pass
    if "\n" in s:
        items = [row.strip().strip('"\'' ) for row in s.splitlines() if row.strip()]
        if len(items) > 1:
            return items
    if ";" in s and s.count(";") >= 1:
        items = [it.strip().strip('"\'' ) for it in s.split(";") if it.strip()]
        if len(items) > 1:
            return items
    if "|" in s and s.count("|") >= 1:
        items = [it.strip().strip('"\'' ) for it in s.split("|") if it.strip()]
        if len(items) > 1:
            return items
    m = re.findall(r"'([^']+)'", s)
    if m and len(m) > 1:
        return [mi.strip() for mi in m]
    if s.count(",") >= 3:
        parts = [p.strip().strip('"\'' ) for p in s.split(",") if p.strip()]
        if sum(1 for p in parts if len(p) > 2) >= 2:
            return parts
    cleaned = unidecode.unidecode(s).strip()
    return [cleaned] if cleaned else []

def ingredient_parser(ingreds):
    """

    This function takes in a list (but it is a string as it comes from pandas dataframe) of
       ingredients and performs some preprocessing.
       For example:

       input = '['1 x 1.6kg whole duck', '2 heaped teaspoons Chinese five-spice powder', '1 clementine',
                 '6 fresh bay leaves', 'GRAVY', '', '1 bulb of garlic', '2 carrots', '2 red onions',
                 '3 tablespoons plain flour', '100 ml Marsala', '1 litre organic chicken stock']'

       output = ['duck', 'chinese five spice powder', 'clementine', 'fresh bay leaf', 'gravy', 'garlic',
                 'carrot', 'red onion', 'plain flour', 'marsala', 'organic chicken stock']

    """
    measures = [
        "teaspoon",
        "t",
        "tsp.",
        "tablespoon",
        "T",
        "tbl.",
        "tb",
        "tbsp.",
        "fluid ounce",
        "fl oz",
        "gill",
        "cup",
        "c",
        "pint",
        "p",
        "pt",
        "fl pt",
        "quart",
        "q",
        "qt",
        "fl qt",
        "gallon",
        "g",
        "gal",
        "ml",
        "milliliter",
        "millilitre",
        "cc",
        "mL",
        "l",
        "liter",
        "litre",
        "L",
        "dl",
        "deciliter",
        "decilitre",
        "dL",
        "bulb",
        "level",
        "heaped",
        "rounded",
        "whole",
        "pinch",
        "medium",
        "slice",
        "pound",
        "lb",
        "#",
        "ounce",
        "oz",
        "mg",
        "milligram",
        "milligramme",
        "g",
        "gram",
        "gramme",
        "kg",
        "kilogram",
        "kilogramme",
        "x",
        "of",
        "mm",
        "millimetre",
        "millimeter",
        "cm",
        "centimeter",
        "centimetre",
        "m",
        "meter",
        "metre",
        "inch",
        "in",
        "milli",
        "centi",
        "deci",
        "hecto",
        "kilo",
    ]
    words_to_remove = [
        "fresh",
        "minced",
        "chopped" "oil",
        "a",
        "red",
        "bunch",
        "and",
        "clove",
        "or",
        "leaf",
        "chilli",
        "large",
        "extra",
        "sprig",
        "ground",
        "handful",
        "free",
        "small",
        "pepper",
        "virgin",
        "range",
        "from",
        "dried",
        "sustainable",
        "black",
        "peeled",
        "higher",
        "welfare",
        "seed",
        "for",
        "finely",
        "freshly",
        "sea",
        "quality",
        "white",
        "ripe",
        "few",
        "piece",
        "source",
        "to",
        "organic",
        "flat",
        "smoked",
        "ginger",
        "sliced",
        "green",
        "picked",
        "the",
        "stick",
        "plain",
        "plus",
        "mixed",
        "mint",
        "bay",
        "basil",
        "your",
        "cumin",
        "optional",
        "fennel",
        "serve",
        "mustard",
        "unsalted",
        "baby",
        "paprika",
        "fat",
        "ask",
        "natural",
        "skin",
        "roughly",
        "into",
        "such",
        "cut",
        "good",
        "brown",
        "grated",
        "trimmed",
        "oregano",
        "powder",
        "yellow",
        "dusting",
        "knob",
        "frozen",
        "on",
        "deseeded",
        "low",
        "runny",
        "balsamic",
        "cooked",
        "streaky",
        "nutmeg",
        "sage",
        "rasher",
        "zest",
        "pin",
        "groundnut",
        "breadcrumb",
        "turmeric",
        "halved",
        "grating",
        "stalk",
        "light",
        "tinned",
        "dry",
        "soft",
        "rocket",
        "bone",
        "colour",
        "washed",
        "skinless",
        "leftover",
        "splash",
        "removed",
        "dijon",
        "thick",
        "big",
        "hot",
        "drained",
        "sized",
        "chestnut",
        "watercress",
        "fishmonger",
        "english",
        "dill",
        "caper",
        "raw",
        "worcestershire",
        "flake",
        "cider",
        "cayenne",
        "tbsp",
        "leg",
        "pine",
        "wild",
        "if",
        "fine",
        "herb",
        "almond",
        "shoulder",
        "cube",
        "dressing",
        "with",
        "chunk",
        "spice",
        "thumb",
        "garam",
        "new",
        "little",
        "punnet",
        "peppercorn",
        "shelled",
        "saffron",
        "other" "chopped",
        "salt",
        "olive",
        "taste",
        "can",
        "sauce",
        "water",
        "diced",
        "package",
        "italian",
        "shredded",
        "divided",
        "parsley",
        "vinegar",
        "all",
        "purpose",
        "crushed",
        "juice",
        "more",
        "coriander",
        "bell",
        "needed",
        "thinly",
        "boneless",
        "half",
        "thyme",
        "cubed",
        "cinnamon",
        "cilantro",
        "jar",
        "seasoning",
        "rosemary",
        "extract",
        "sweet",
        "baking",
        "beaten",
        "heavy",
        "seeded",
        "tin",
        "vanilla",
        "uncooked",
        "crumb",
        "style",
        "thin",
        "nut",
        "coarsely",
        "spring",
        "chili",
        "cornstarch",
        "strip",
        "cardamom",
        "rinsed",
        "honey",
        "cherry",
        "root",
        "quartered",
        "head",
        "softened",
        "container",
        "crumbled",
        "frying",
        "lean",
        "cooking",
        "roasted",
        "warm",
        "whipping",
        "thawed",
        "corn",
        "pitted",
        "sun",
        "kosher",
        "bite",
        "toasted",
        "lasagna",
        "split",
        "melted",
        "degree",
        "lengthwise",
        "romano",
        "packed",
        "pod",
        "anchovy",
        "rom",
        "prepared",
        "juiced",
        "fluid",
        "floret",
        "room",
        "active",
        "seasoned",
        "mix",
        "deveined",
        "lightly",
        "anise",
        "thai",
        "size",
        "unsweetened",
        "torn",
        "wedge",
        "sour",
        "basmati",
        "marinara",
        "dark",
        "temperature",
        "garnish",
        "bouillon",
        "loaf",
        "shell",
        "reggiano",
        "canola",
        "parmigiano",
        "round",
        "canned",
        "ghee",
        "crust",
        "long",
        "broken",
        "ketchup",
        "bulk",
        "cleaned",
        "condensed",
        "sherry",
        "provolone",
        "cold",
        "soda",
        "cottage",
        "spray",
        "tamarind",
        "pecorino",
        "shortening",
        "part",
        "bottle",
        "sodium",
        "cocoa",
        "grain",
        "french",
        "roast",
        "stem",
        "link",
        "firm",
        "asafoetida",
        "mild",
        "dash",
        "boiling",
        "oil",
        "chopped",
        "vegetable oil",
        "chopped oil",
        "garlic",
        "skin off",
        "bone out" "from sustrainable sources",
    ]
    # The ingredient list is now a string so we need to turn it back into a list. We use ast.literal_eval
    if isinstance(ingreds, list):
        ingredients = ingreds
    else:
        ingredients = safe_parse_ingredient_list(ingreds)
    # We first get rid of all the punctuation. We make use of str.maketrans. It takes three input
    # arguments 'x', 'y', 'z'. 'x' and 'y' must be equal-length strings and characters in 'x'
    # are replaced by characters in 'y'. 'z' is a string (string.punctuation here) where each character
    #  in the string is mapped to None.
    translator = str.maketrans("", "", string.punctuation)
    lemmatizer = WordNetLemmatizer()
    ingred_list = []
    for i in ingredients:
        i.translate(translator)
        # We split up with hyphens as well as spaces
        part = i.split("|", 1)[0].strip()
        item = part[3:] if len(part) > 3 else part
        items = re.split(" |-", item)
        items = [word for word in items if word.isalpha()]
        # Turn everything to lowercase
        items = [word.lower() for word in items]

        # remove accents
        items = [
            unidecode.unidecode(word) for word in items
        ]  #''.join((c for c in unicodedata.normalize('NFD', items) if unicodedata.category(c) != 'Mn'))
        # Lemmatize words so we can compare words to measuring words
        items = [lemmatizer.lemmatize(word) for word in items]
        # Gets rid of measuring words/phrases, e.g. heaped teaspoon
        items = [word for word in items if word not in measures]
        # Get rid of common easy words
        items = [word for word in items if word not in words_to_remove]
        if items:
            processed_item = " ".join(items)
            # Проверяем, нет ли уже такого элемента в ingred_list
            if processed_item not in ingred_list:
                ingred_list.append(processed_item)    # ingred_list = " ".join(ingred_list)
    return ingred_list

# ------------------ SimpleWordLookup: загрузка npz слов и векторов ------------------
class SimpleWordLookup:

    def __init__(self, npz_path):
        data = np.load(npz_path, allow_pickle=True)
        raw_words = data["words"]
        # декодируем байты, если нужно
        self.words = [w.decode("utf-8") if isinstance(w, (bytes, bytearray)) else str(w) for w in raw_words]
        self.vectors = data["vectors"]
        # индекс для быстрого поиска
        self.key_to_index = {w: i for i, w in enumerate(self.words)}
        self.vector_size = int(self.vectors.shape[1])

    def get_vector(self, word):
        idx = self.key_to_index.get(word)
        if idx is None:
            return None
        return self.vectors[idx]

    # минимальная обёртка, чтобы код, использующий word_model.wv.key_to_index и get_vector, работал
    @property
    def wv(self):
        class WV:
            def __init__(self, key_to_index, vector_size, vectors):
                self.key_to_index = key_to_index
                self.vector_size = vector_size
                self._vectors = vectors
            def get_vector(self, w):
                idx = self.key_to_index.get(w)
                if idx is None:
                    return None
                return self._vectors[idx]
        return WV(self.key_to_index, self.vector_size, self.vectors)

# ------------------ TfidfEmbeddingVectorizer (как у вас) ------------------
class TfidfEmbeddingVectorizer(object):
    def __init__(self, word_model):
        # word_model здесь — объект с интерфейсом .wv.key_to_index и .wv.get_vector
        self.word_model = word_model
        self.word_idf_weight = defaultdict(lambda: 1.0)
        self.vector_size = word_model.wv.vector_size

    def transform(self, docs):
        if not docs:
            return np.empty((0, self.vector_size), dtype=float)
        if isinstance(docs, list) and docs and isinstance(docs[0], str):
            docs = [docs]
        return self._doc_average_list(docs)

    def _doc_average(self, doc):
        mean = []
        for word in doc:
            if word in self.word_model.wv.key_to_index:
                vec = self.word_model.wv.get_vector(word)
                if vec is None:
                    continue
                mean.append(vec * self.word_idf_weight[word])
        if not mean:
            return np.zeros(self.vector_size, dtype=float)
        return np.array(mean).mean(axis=0)

    def _doc_average_list(self, docs):
        return np.vstack([self._doc_average(doc) for doc in docs])

    @classmethod
    def load(cls, path_prefix, word_lookup):
        """
        path_prefix: путь без суффикса, например /data/data/.../files/tfidf_word2vec_tfidf
        word_lookup: объект SimpleWordLookup (или совместимый)
        """
        idf_path = f"{path_prefix}_idf.joblib"
        meta_path = f"{path_prefix}_meta.json"
        idf_dict = {}
        try:
            idf_dict = joblib.load(idf_path)
        except Exception:
            idf_dict = {}
        try:
            with open(meta_path, "r", encoding="utf-8") as f:
                meta = json.load(f)
        except Exception:
            meta = {"vector_size": word_lookup.vector_size}
        inst = cls(word_lookup)
        if idf_dict:
            max_idf = max(idf_dict.values())
        else:
            max_idf = 1.0
        inst.word_idf_weight = defaultdict(lambda: max_idf, idf_dict)
        inst.vector_size = int(meta.get("vector_size", word_lookup.vector_size))
        return inst

# ------------------ глобальные кеши ------------------
_GLOBAL_LOOKUP = None   # SimpleWordLookup instance
_GLOBAL_VEC = None

def load_artifacts(word_vectors_npz_path, tfidf_prefix):
    """
    word_vectors_npz_path: полный путь к word_vectors.npz
    tfidf_prefix: путь без суффикса, например /data/data/.../files/tfidf_word2vec_tfidf
    """
    global _GLOBAL_LOOKUP, _GLOBAL_VEC
    if _GLOBAL_LOOKUP is None:
        if not os.path.exists(word_vectors_npz_path):
            raise FileNotFoundError(f"word vectors file not found: {word_vectors_npz_path}")
        _GLOBAL_LOOKUP = SimpleWordLookup(word_vectors_npz_path)
    if _GLOBAL_VEC is None:
        _GLOBAL_VEC = TfidfEmbeddingVectorizer.load(tfidf_prefix, _GLOBAL_LOOKUP)
    return True

# ------------------ публичные функции для Java ------------------
def parse_and_vectorize(word_vectors_npz_path, tfidf_prefix, raw_input):
    """
    Возвращает JSON: {"tokens": [...], "parsed": [...], "vector": [...], "dim": n}
    word_vectors_npz_path: путь к word_vectors.npz в файловой системе приложения
    tfidf_prefix: путь без суффикса _idf.joblib
    raw_input: строка или список (передаётся из Java)
    """
    load_artifacts(word_vectors_npz_path, tfidf_prefix)
    # используем ваш ingredient_parser (он принимает строку или список)
    parsed_items = ingredient_parser(raw_input)
    tokens = []
    for p in parsed_items:
        parts = re.sub(r"[^\w\s\-']", " ", p.lower()).split()
        tokens.extend([t for t in parts if t])
    vecs = _GLOBAL_VEC.transform(tokens)
    vector = vecs[0].astype(float).tolist() if vecs.shape[0] > 0 else []
    return json.dumps({"tokens": tokens, "parsed": parsed_items, "vector": vector, "dim": len(vector)})

def vector_from_tokens(word_vectors_npz_path, tfidf_prefix, token_list):
    load_artifacts(word_vectors_npz_path, tfidf_prefix)
    vecs = _GLOBAL_VEC.transform(token_list)
    vector = vecs[0].astype(float).tolist() if vecs.shape[0] > 0 else []
    return json.dumps({"vector": vector, "dim": len(vector)})
