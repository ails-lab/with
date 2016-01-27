/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package model.basicDataTypes;
/**
 * The languages are added using the alpha2 code preferably 
 * but also another code can be used to refer to it. We define
 * how is it called in English and after it all of the codes 
 * used to refer to it, considering the first one as the default
 * code used in the WITH system. 
 */
public enum UniqueLanguage {

	/**
	* Afar
	*/
	AA("Afar", "aa", "aar"),
	/**
	* Abkhazian
	*/
	AB("Abkhazian", "ab", "abk"),
	/**
	* Afrikaans
	*/
	AF("Afrikaans", "af", "afr"),
	/**
	* Akan
	*/
	AK("Akan", "ak", "aka"),
	/**
	* Albanian
	*/
	SQ("Albanian", "sq", "alb", "sqi"),
	/**
	* Amharic
	*/
	AM("Amharic", "am", "amh"),
	/**
	* Arabic
	*/
	AR("Arabic", "ar", "ara"),
	/**
	* Aragonese
	*/
	AN("Aragonese", "an", "arg"),
	/**
	* Armenian
	*/
	HY("Armenian", "hy", "arm", "hye"),
	/**
	* Assamese
	*/
	AS("Assamese", "as", "asm"),
	/**
	* Avaric
	*/
	AV("Avaric", "av", "ava"),
	/**
	* Avestan
	*/
	AE("Avestan", "ae", "ave"),
	/**
	* Aymara
	*/
	AY("Aymara", "ay", "aym"),
	/**
	* Azerbaijani
	*/
	AZ("Azerbaijani", "az", "aze"),
	/**
	* Bashkir
	*/
	BA("Bashkir", "ba", "bak"),
	/**
	* Bambara
	*/
	BM("Bambara", "bm", "bam"),
	/**
	* Basque
	*/
	EU("Basque", "eu", "baq", "eus"),
	/**
	* Belarusian
	*/
	BE("Belarusian", "be", "bel"),
	/**
	* Bengali
	*/
	BN("Bengali", "bn", "ben"),
	/**
	* Bihari languages
	*/
	BH("Bihari languages", "bh", "bih"),
	/**
	* Bislama
	*/
	BI("Bislama", "bi", "bis"),
	/**
	* Bosnian
	*/
	BS("Bosnian", "bs", "bos"),
	/**
	* Breton
	*/
	BR("Breton", "br", "bre"),
	/**
	* Bulgarian
	*/
	BG("Bulgarian", "bg", "bul"),
	/**
	* Burmese
	*/
	MY("Burmese", "my", "bur", "mya"),
	/**
	* Catalan; Valencian
	*/
	CA("Catalan; Valencian", "ca", "cat"),
	/**
	* Chamorro
	*/
	CH("Chamorro", "ch", "cha"),
	/**
	* Chechen
	*/
	CE("Chechen", "ce", "che"),
	/**
	* Chinese
	*/
	ZH("Chinese", "zh", "chi", "zho"),
	/**
	* Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic
	*/
	CU("Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic", "cu", "chu"),
	/**
	* Chuvash
	*/
	CV("Chuvash", "cv", "chv"),
	/**
	* Cornish
	*/
	KW("Cornish", "kw", "cor"),
	/**
	* Corsican
	*/
	CO("Corsican", "co", "cos"),
	/**
	* Cree
	*/
	CR("Cree", "cr", "cre"),
	/**
	* Czech
	*/
	CS("Czech", "cs", "cze", "ces"),
	/**
	* Danish
	*/
	DA("Danish", "da", "dan"),
	/**
	* Divehi; Dhivehi; Maldivian
	*/
	DV("Divehi; Dhivehi; Maldivian", "dv", "div"),
	/**
	* Dutch; Flemish
	*/
	NL("Dutch; Flemish", "nl", "dut", "nld"),
	/**
	* Dzongkha
	*/
	DZ("Dzongkha", "dz", "dzo"),
	/**
	* English
	*/
	EN("English", "en", "eng"),
	/**
	* Esperanto
	*/
	EO("Esperanto", "eo", "epo"),
	/**
	* Estonian
	*/
	ET("Estonian", "et", "est"),
	/**
	* Ewe
	*/
	EE("Ewe", "ee", "ewe"),
	/**
	* Faroese
	*/
	FO("Faroese", "fo", "fao"),
	/**
	* Fijian
	*/
	FJ("Fijian", "fj", "fij"),
	/**
	* Finnish
	*/
	FI("Finnish", "fi", "fin"),
	/**
	* French
	*/
	FR("French", "fr", "fre", "fra"),
	/**
	* Western Frisian
	*/
	FY("Western Frisian", "fy", "fry"),
	/**
	* Fulah
	*/
	FF("Fulah", "ff", "ful"),
	/**
	* Georgian
	*/
	KA("Georgian", "ka", "geo", "kat"),
	/**
	* German
	*/
	DE("German", "de", "ger", "deu"),
	/**
	* Gaelic; Scottish Gaelic
	*/
	GD("Gaelic; Scottish Gaelic", "gd", "gla"),
	/**
	* Irish
	*/
	GA("Irish", "ga", "gle"),
	/**
	* Galician
	*/
	GL("Galician", "gl", "glg"),
	/**
	* Manx
	*/
	GV("Manx", "gv", "glv"),
	/**
	* Greek, Modern (1453-)
	*/
	EL("Greek, Modern (1453-)", "el", "gre", "ell"),
	/**
	* Guarani
	*/
	GN("Guarani", "gn", "grn"),
	/**
	* Gujarati
	*/
	GU("Gujarati", "gu", "guj"),
	/**
	* Haitian; Haitian Creole
	*/
	HT("Haitian; Haitian Creole", "ht", "hat"),
	/**
	* Hausa
	*/
	HA("Hausa", "ha", "hau"),
	/**
	* Hebrew
	*/
	HE("Hebrew", "he", "heb"),
	/**
	* Herero
	*/
	HZ("Herero", "hz", "her"),
	/**
	* Hindi
	*/
	HI("Hindi", "hi", "hin"),
	/**
	* Hiri Motu
	*/
	HO("Hiri Motu", "ho", "hmo"),
	/**
	* Croatian
	*/
	HR("Croatian", "hr", "hrv"),
	/**
	* Hungarian
	*/
	HU("Hungarian", "hu", "hun"),
	/**
	* Igbo
	*/
	IG("Igbo", "ig", "ibo"),
	/**
	* Icelandic
	*/
	IS("Icelandic", "is", "ice", "isl"),
	/**
	* Ido
	*/
	IO("Ido", "io", "ido"),
	/**
	* Sichuan Yi; Nuosu
	*/
	II("Sichuan Yi; Nuosu", "ii", "iii"),
	/**
	* Inuktitut
	*/
	IU("Inuktitut", "iu", "iku"),
	/**
	* Interlingue; Occidental
	*/
	IE("Interlingue; Occidental", "ie", "ile"),
	/**
	* Interlingua (International Auxiliary Language Association)
	*/
	IA("Interlingua (International Auxiliary Language Association)", "ia", "ina"),
	/**
	* Indonesian
	*/
	ID("Indonesian", "id", "ind"),
	/**
	* Inupiaq
	*/
	IK("Inupiaq", "ik", "ipk"),
	/**
	* Italian
	*/
	IT("Italian", "it", "ita"),
	/**
	* Javanese
	*/
	JV("Javanese", "jv", "jav"),
	/**
	* Japanese
	*/
	JA("Japanese", "ja", "jpn"),
	/**
	* Kalaallisut; Greenlandic
	*/
	KL("Kalaallisut; Greenlandic", "kl", "kal"),
	/**
	* Kannada
	*/
	KN("Kannada", "kn", "kan"),
	/**
	* Kashmiri
	*/
	KS("Kashmiri", "ks", "kas"),
	/**
	* Kanuri
	*/
	KR("Kanuri", "kr", "kau"),
	/**
	* Kazakh
	*/
	KK("Kazakh", "kk", "kaz"),
	/**
	* Central Khmer
	*/
	KM("Central Khmer", "km", "khm"),
	/**
	* Kikuyu; Gikuyu
	*/
	KI("Kikuyu; Gikuyu", "ki", "kik"),
	/**
	* Kinyarwanda
	*/
	RW("Kinyarwanda", "rw", "kin"),
	/**
	* Kirghiz; Kyrgyz
	*/
	KY("Kirghiz; Kyrgyz", "ky", "kir"),
	/**
	* Komi
	*/
	KV("Komi", "kv", "kom"),
	/**
	* Kongo
	*/
	KG("Kongo", "kg", "kon"),
	/**
	* Korean
	*/
	KO("Korean", "ko", "kor"),
	/**
	* Kuanyama; Kwanyama
	*/
	KJ("Kuanyama; Kwanyama", "kj", "kua"),
	/**
	* Kurdish
	*/
	KU("Kurdish", "ku", "kur"),
	/**
	* Lao
	*/
	LO("Lao", "lo", "lao"),
	/**
	* Latin
	*/
	LA("Latin", "la", "lat"),
	/**
	* Latvian
	*/
	LV("Latvian", "lv", "lav"),
	/**
	* Limburgan; Limburger; Limburgish
	*/
	LI("Limburgan; Limburger; Limburgish", "li", "lim"),
	/**
	* Lingala
	*/
	LN("Lingala", "ln", "lin"),
	/**
	* Lithuanian
	*/
	LT("Lithuanian", "lt", "lit"),
	/**
	* Luxembourgish; Letzeburgesch
	*/
	LB("Luxembourgish; Letzeburgesch", "lb", "ltz"),
	/**
	* Luba-Katanga
	*/
	LU("Luba-Katanga", "lu", "lub"),
	/**
	* Ganda
	*/
	LG("Ganda", "lg", "lug"),
	/**
	* Macedonian
	*/
	MK("Macedonian", "mk", "mac", "mkd"),
	/**
	* Marshallese
	*/
	MH("Marshallese", "mh", "mah"),
	/**
	* Malayalam
	*/
	ML("Malayalam", "ml", "mal"),
	/**
	* Maori
	*/
	MI("Maori", "mi", "mao", "mri"),
	/**
	* Marathi
	*/
	MR("Marathi", "mr", "mar"),
	/**
	* Malay
	*/
	MS("Malay", "ms", "may", "msa"),
	/**
	* Malagasy
	*/
	MG("Malagasy", "mg", "mlg"),
	/**
	* Maltese
	*/
	MT("Maltese", "mt", "mlt"),
	/**
	* Mongolian
	*/
	MN("Mongolian", "mn", "mon"),
	/**
	* Nauru
	*/
	NA("Nauru", "na", "nau"),
	/**
	* Navajo; Navaho
	*/
	NV("Navajo; Navaho", "nv", "nav"),
	/**
	* Ndebele, South; South Ndebele
	*/
	NR("Ndebele, South; South Ndebele", "nr", "nbl"),
	/**
	* Ndebele, North; North Ndebele
	*/
	ND("Ndebele, North; North Ndebele", "nd", "nde"),
	/**
	* Ndonga
	*/
	NG("Ndonga", "ng", "ndo"),
	/**
	* Nepali
	*/
	NE("Nepali", "ne", "nep"),
	/**
	* Norwegian Nynorsk; Nynorsk, Norwegian
	*/
	NN("Norwegian Nynorsk; Nynorsk, Norwegian", "nn", "nno"),
	/**
	* Bokmål, Norwegian; Norwegian Bokmål
	*/
	NB("Bokmål, Norwegian; Norwegian Bokmål", "nb", "nob"),
	/**
	* Norwegian
	*/
	NO("Norwegian", "no", "nor"),
	/**
	* Chichewa; Chewa; Nyanja
	*/
	NY("Chichewa; Chewa; Nyanja", "ny", "nya"),
	/**
	* Occitan (post 1500); Provençal
	*/
	OC("Occitan (post 1500); Provençal", "oc", "oci"),
	/**
	* Ojibwa
	*/
	OJ("Ojibwa", "oj", "oji"),
	/**
	* Oriya
	*/
	OR("Oriya", "or", "ori"),
	/**
	* Oromo
	*/
	OM("Oromo", "om", "orm"),
	/**
	* Ossetian; Ossetic
	*/
	OS("Ossetian; Ossetic", "os", "oss"),
	/**
	* Panjabi; Punjabi
	*/
	PA("Panjabi; Punjabi", "pa", "pan"),
	/**
	* Persian
	*/
	FA("Persian", "fa", "per", "fas"),
	/**
	* Pali
	*/
	PI("Pali", "pi", "pli"),
	/**
	* Polish
	*/
	PL("Polish", "pl", "pol"),
	/**
	* Portuguese
	*/
	PT("Portuguese", "pt", "por"),
	/**
	* Pushto; Pashto
	*/
	PS("Pushto; Pashto", "ps", "pus"),
	/**
	* Quechua
	*/
	QU("Quechua", "qu", "que"),
	/**
	* Romansh
	*/
	RM("Romansh", "rm", "roh"),
	/**
	* Romanian; Moldavian; Moldovan
	*/
	RO("Romanian; Moldavian; Moldovan", "ro", "rum", "ron"),
	/**
	* Rundi
	*/
	RN("Rundi", "rn", "run"),
	/**
	* Russian
	*/
	RU("Russian", "ru", "rus"),
	/**
	* Sango
	*/
	SG("Sango", "sg", "sag"),
	/**
	* Sanskrit
	*/
	SA("Sanskrit", "sa", "san"),
	/**
	* Sinhala; Sinhalese
	*/
	SI("Sinhala; Sinhalese", "si", "sin"),
	/**
	* Slovak
	*/
	SK("Slovak", "sk", "slo", "slk"),
	/**
	* Slovenian
	*/
	SL("Slovenian", "sl", "slv"),
	/**
	* Northern Sami
	*/
	SE("Northern Sami", "se", "sme"),
	/**
	* Samoan
	*/
	SM("Samoan", "sm", "smo"),
	/**
	* Shona
	*/
	SN("Shona", "sn", "sna"),
	/**
	* Sindhi
	*/
	SD("Sindhi", "sd", "snd"),
	/**
	* Somali
	*/
	SO("Somali", "so", "som"),
	/**
	* Sotho, Southern
	*/
	ST("Sotho, Southern", "st", "sot"),
	/**
	* Spanish; Castilian
	*/
	ES("Spanish; Castilian", "es", "spa"),
	/**
	* Sardinian
	*/
	SC("Sardinian", "sc", "srd"),
	/**
	* Serbian
	*/
	SR("Serbian", "sr", "srp"),
	/**
	* Swati
	*/
	SS("Swati", "ss", "ssw"),
	/**
	* Sundanese
	*/
	SU("Sundanese", "su", "sun"),
	/**
	* Swahili
	*/
	SW("Swahili", "sw", "swa"),
	/**
	* Swedish
	*/
	SV("Swedish", "sv", "swe"),
	/**
	* Tahitian
	*/
	TY("Tahitian", "ty", "tah"),
	/**
	* Tamil
	*/
	TA("Tamil", "ta", "tam"),
	/**
	* Tatar
	*/
	TT("Tatar", "tt", "tat"),
	/**
	* Telugu
	*/
	TE("Telugu", "te", "tel"),
	/**
	* Tajik
	*/
	TG("Tajik", "tg", "tgk"),
	/**
	* Tagalog
	*/
	TL("Tagalog", "tl", "tgl"),
	/**
	* Thai
	*/
	TH("Thai", "th", "tha"),
	/**
	* Tibetan
	*/
	BO("Tibetan", "bo", "tib", "bod"),
	/**
	* Tigrinya
	*/
	TI("Tigrinya", "ti", "tir"),
	/**
	* Tonga (Tonga Islands)
	*/
	TO("Tonga (Tonga Islands)", "to", "ton"),
	/**
	* Tswana
	*/
	TN("Tswana", "tn", "tsn"),
	/**
	* Tsonga
	*/
	TS("Tsonga", "ts", "tso"),
	/**
	* Turkmen
	*/
	TK("Turkmen", "tk", "tuk"),
	/**
	* Turkish
	*/
	TR("Turkish", "tr", "tur"),
	/**
	* Twi
	*/
	TW("Twi", "tw", "twi"),
	/**
	* Uighur; Uyghur
	*/
	UG("Uighur; Uyghur", "ug", "uig"),
	/**
	* Ukrainian
	*/
	UK("Ukrainian", "uk", "ukr"),
	/**
	* Urdu
	*/
	UR("Urdu", "ur", "urd"),
	/**
	* Uzbek
	*/
	UZ("Uzbek", "uz", "uzb"),
	/**
	* Venda
	*/
	VE("Venda", "ve", "ven"),
	/**
	* Vietnamese
	*/
	VI("Vietnamese", "vi", "vie"),
	/**
	* Volapük
	*/
	VO("Volapük", "vo", "vol"),
	/**
	* Welsh
	*/
	CY("Welsh", "cy", "wel", "cym"),
	/**
	* Walloon
	*/
	WA("Walloon", "wa", "wln"),
	/**
	* Wolof
	*/
	WO("Wolof", "wo", "wol"),
	/**
	* Xhosa
	*/
	XH("Xhosa", "xh", "xho"),
	/**
	* Yiddish
	*/
	YI("Yiddish", "yi", "yid"),
	/**
	* Yoruba
	*/
	YO("Yoruba", "yo", "yor"),
	/**
	* Zhuang; Chuang
	*/
	ZA("Zhuang; Chuang", "za", "zha"),
	/**
	* Zulu
	*/
	ZU("Zulu", "zu", "zul"),


	DEF("default", "df", "def"),
	
	UNKNOWN("Unknown", "un", "unk");

	
	
	
	private String name;
	private String[] codes;
	
	private UniqueLanguage(String name, String... codes) {
		this.name = name;
		this.codes = codes;
	}

	public String[] getCodes() {
		return codes;
	}

	public String getDefaultCode(){
		return codes[0];
	}
	
	public String getName() {
		return name;
	}

	public static UniqueLanguage getLanguage(String code){
		for (UniqueLanguage lang : UniqueLanguage.values()) {
			for (String langcode: lang.getCodes()) {
				if (code.equals(langcode))
					return lang;
			}
		}
		return UniqueLanguage.UNKNOWN;
	}

}
