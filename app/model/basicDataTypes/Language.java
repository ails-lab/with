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

import play.Logger;
import sources.core.Utils;

/**
 * The languages are added using the alpha2 code preferably 
 * but also another code can be used to refer to it. We define
 * how is it called in English and after it all of the codes 
 * used to refer to it, considering the first one as the default
 * code used in the WITH system. 
 */
public enum Language {

	/**
	* Afar
	*/
	AA("Afar", "aa", "aar"),
	/**
	* Abkhazian
	*/
	AB("Abkhazian", "ab", "abk"),
	/**
	* Achinese
	*/
	ACE("Achinese", "ace"),
	/**
	* Acoli
	*/
	ACH("Acoli", "ach"),
	/**
	* Adangme
	*/
	ADA("Adangme", "ada"),
	/**
	* Adyghe; Adygei
	*/
	ADY("Adyghe; Adygei", "ady"),
	/**
	* Afro-Asiatic languages
	*/
	AFA("Afro-Asiatic languages", "afa"),
	/**
	* Afrihili
	*/
	AFH("Afrihili", "afh"),
	/**
	* Afrikaans
	*/
	AF("Afrikaans", "af", "afr"),
	/**
	* Ainu
	*/
	AIN("Ainu", "ain"),
	/**
	* Akan
	*/
	AK("Akan", "ak", "aka"),
	/**
	* Akkadian
	*/
	AKK("Akkadian", "akk"),
	/**
	* Albanian
	*/
	SQ("Albanian", "sq", "alb", "sqi"),
	/**
	* Aleut
	*/
	ALE("Aleut", "ale"),
	/**
	* Algonquian languages
	*/
	ALG("Algonquian languages", "alg"),
	/**
	* Southern Altai
	*/
	ALT("Southern Altai", "alt"),
	/**
	* Amharic
	*/
	AM("Amharic", "am", "amh"),
	/**
	* English, Old (ca.450-1100)
	*/
	ANG("English, Old (ca.450-1100)", "ang"),
	/**
	* Angika
	*/
	ANP("Angika", "anp"),
	/**
	* Apache languages
	*/
	APA("Apache languages", "apa"),
	/**
	* Arabic
	*/
	AR("Arabic", "ar", "ara"),
	/**
	* Official Aramaic (700-300 BCE); Imperial Aramaic (700-300 BCE)
	*/
	ARC("Official Aramaic (700-300 BCE); Imperial Aramaic (700-300 BCE)", "arc"),
	/**
	* Aragonese
	*/
	AN("Aragonese", "an", "arg"),
	/**
	* Armenian
	*/
	HY("Armenian", "hy", "arm", "hye"),
	/**
	* Mapudungun; Mapuche
	*/
	ARN("Mapudungun; Mapuche", "arn"),
	/**
	* Arapaho
	*/
	ARP("Arapaho", "arp"),
	/**
	* Artificial languages
	*/
	ART("Artificial languages", "art"),
	/**
	* Arawak
	*/
	ARW("Arawak", "arw"),
	/**
	* Assamese
	*/
	AS("Assamese", "as", "asm"),
	/**
	* Asturian; Bable; Leonese; Asturleonese
	*/
	AST("Asturian; Bable; Leonese; Asturleonese", "ast"),
	/**
	* Athapascan languages
	*/
	ATH("Athapascan languages", "ath"),
	/**
	* Australian languages
	*/
	AUS("Australian languages", "aus"),
	/**
	* Avaric
	*/
	AV("Avaric", "av", "ava"),
	/**
	* Avestan
	*/
	AE("Avestan", "ae", "ave"),
	/**
	* Awadhi
	*/
	AWA("Awadhi", "awa"),
	/**
	* Aymara
	*/
	AY("Aymara", "ay", "aym"),
	/**
	* Azerbaijani
	*/
	AZ("Azerbaijani", "az", "aze"),
	/**
	* Banda languages
	*/
	BAD("Banda languages", "bad"),
	/**
	* Bamileke languages
	*/
	BAI("Bamileke languages", "bai"),
	/**
	* Bashkir
	*/
	BA("Bashkir", "ba", "bak"),
	/**
	* Baluchi
	*/
	BAL("Baluchi", "bal"),
	/**
	* Bambara
	*/
	BM("Bambara", "bm", "bam"),
	/**
	* Balinese
	*/
	BAN("Balinese", "ban"),
	/**
	* Basque
	*/
	EU("Basque", "eu", "baq", "eus"),
	/**
	* Basa
	*/
	BAS("Basa", "bas"),
	/**
	* Baltic languages
	*/
	BAT("Baltic languages", "bat"),
	/**
	* Beja; Bedawiyet
	*/
	BEJ("Beja; Bedawiyet", "bej"),
	/**
	* Belarusian
	*/
	BE("Belarusian", "be", "bel"),
	/**
	* Bemba
	*/
	BEM("Bemba", "bem"),
	/**
	* Bengali
	*/
	BN("Bengali", "bn", "ben"),
	/**
	* Berber languages
	*/
	BER("Berber languages", "ber"),
	/**
	* Bhojpuri
	*/
	BHO("Bhojpuri", "bho"),
	/**
	* Bihari languages
	*/
	BH("Bihari languages", "bh", "bih"),
	/**
	* Bikol
	*/
	BIK("Bikol", "bik"),
	/**
	* Bini; Edo
	*/
	BIN("Bini; Edo", "bin"),
	/**
	* Bislama
	*/
	BI("Bislama", "bi", "bis"),
	/**
	* Siksika
	*/
	BLA("Siksika", "bla"),
	/**
	* Bantu (Other)
	*/
	BNT("Bantu (Other)", "bnt"),
	/**
	* Bosnian
	*/
	BS("Bosnian", "bs", "bos"),
	/**
	* Braj
	*/
	BRA("Braj", "bra"),
	/**
	* Breton
	*/
	BR("Breton", "br", "bre"),
	/**
	* Batak languages
	*/
	BTK("Batak languages", "btk"),
	/**
	* Buriat
	*/
	BUA("Buriat", "bua"),
	/**
	* Buginese
	*/
	BUG("Buginese", "bug"),
	/**
	* Bulgarian
	*/
	BG("Bulgarian", "bg", "bul"),
	/**
	* Burmese
	*/
	MY("Burmese", "my", "bur", "mya"),
	/**
	* Blin; Bilin
	*/
	BYN("Blin; Bilin", "byn"),
	/**
	* Caddo
	*/
	CAD("Caddo", "cad"),
	/**
	* Central American Indian languages
	*/
	CAI("Central American Indian languages", "cai"),
	/**
	* Galibi Carib
	*/
	CAR("Galibi Carib", "car"),
	/**
	* Catalan; Valencian
	*/
	CA("Catalan; Valencian", "ca", "cat"),
	/**
	* Caucasian languages
	*/
	CAU("Caucasian languages", "cau"),
	/**
	* Cebuano
	*/
	CEB("Cebuano", "ceb"),
	/**
	* Celtic languages
	*/
	CEL("Celtic languages", "cel"),
	/**
	* Chamorro
	*/
	CH("Chamorro", "ch", "cha"),
	/**
	* Chibcha
	*/
	CHB("Chibcha", "chb"),
	/**
	* Chechen
	*/
	CE("Chechen", "ce", "che"),
	/**
	* Chagatai
	*/
	CHG("Chagatai", "chg"),
	/**
	* Chinese
	*/
	ZH("Chinese", "zh", "chi", "zho"),
	/**
	* Chuukese
	*/
	CHK("Chuukese", "chk"),
	/**
	* Mari
	*/
	CHM("Mari", "chm"),
	/**
	* Chinook jargon
	*/
	CHN("Chinook jargon", "chn"),
	/**
	* Choctaw
	*/
	CHO("Choctaw", "cho"),
	/**
	* Chipewyan; Dene Suline
	*/
	CHP("Chipewyan; Dene Suline", "chp"),
	/**
	* Cherokee
	*/
	CHR("Cherokee", "chr"),
	/**
	* Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic
	*/
	CU("Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic", "cu", "chu"),
	/**
	* Chuvash
	*/
	CV("Chuvash", "cv", "chv"),
	/**
	* Cheyenne
	*/
	CHY("Cheyenne", "chy"),
	/**
	* Chamic languages
	*/
	CMC("Chamic languages", "cmc"),
	/**
	* Coptic
	*/
	COP("Coptic", "cop"),
	/**
	* Cornish
	*/
	KW("Cornish", "kw", "cor"),
	/**
	* Corsican
	*/
	CO("Corsican", "co", "cos"),
	/**
	* Creoles and pidgins, English based
	*/
	CPE("Creoles and pidgins, English based", "cpe"),
	/**
	* Creoles and pidgins, French-based 
	*/
	CPF("Creoles and pidgins, French-based ", "cpf"),
	/**
	* Creoles and pidgins, Portuguese-based 
	*/
	CPP("Creoles and pidgins, Portuguese-based ", "cpp"),
	/**
	* Cree
	*/
	CR("Cree", "cr", "cre"),
	/**
	* Crimean Tatar; Crimean Turkish
	*/
	CRH("Crimean Tatar; Crimean Turkish", "crh"),
	/**
	* Creoles and pidgins 
	*/
	CRP("Creoles and pidgins ", "crp"),
	/**
	* Kashubian
	*/
	CSB("Kashubian", "csb"),
	/**
	* Cushitic languages
	*/
	CUS("Cushitic languages", "cus"),
	/**
	* Czech
	*/
	CS("Czech", "cs", "cze", "ces"),
	/**
	* Dakota
	*/
	DAK("Dakota", "dak"),
	/**
	* Danish
	*/
	DA("Danish", "da", "dan"),
	/**
	* Dargwa
	*/
	DAR("Dargwa", "dar"),
	/**
	* Land Dayak languages
	*/
	DAY("Land Dayak languages", "day"),
	/**
	* Delaware
	*/
	DEL("Delaware", "del"),
	/**
	* Slave (Athapascan)
	*/
	DEN("Slave (Athapascan)", "den"),
	/**
	* Dogrib
	*/
	DGR("Dogrib", "dgr"),
	/**
	* Dinka
	*/
	DIN("Dinka", "din"),
	/**
	* Divehi; Dhivehi; Maldivian
	*/
	DV("Divehi; Dhivehi; Maldivian", "dv", "div"),
	/**
	* Dogri
	*/
	DOI("Dogri", "doi"),
	/**
	* Dravidian languages
	*/
	DRA("Dravidian languages", "dra"),
	/**
	* Lower Sorbian
	*/
	DSB("Lower Sorbian", "dsb"),
	/**
	* Duala
	*/
	DUA("Duala", "dua"),
	/**
	* Dutch, Middle (ca.1050-1350)
	*/
	DUM("Dutch, Middle (ca.1050-1350)", "dum"),
	/**
	* Dutch; Flemish
	*/
	NL("Dutch; Flemish", "nl", "dut", "nld"),
	/**
	* Dyula
	*/
	DYU("Dyula", "dyu"),
	/**
	* Dzongkha
	*/
	DZ("Dzongkha", "dz", "dzo"),
	/**
	* Efik
	*/
	EFI("Efik", "efi"),
	/**
	* Egyptian (Ancient)
	*/
	EGY("Egyptian (Ancient)", "egy"),
	/**
	* Ekajuk
	*/
	EKA("Ekajuk", "eka"),
	/**
	* Elamite
	*/
	ELX("Elamite", "elx"),
	/**
	* English
	*/
	EN("English", "en", "eng", "en-us"),
	/**
	* English, Middle (1100-1500)
	*/
	ENM("English, Middle (1100-1500)", "enm"),
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
	* Ewondo
	*/
	EWO("Ewondo", "ewo"),
	/**
	* Fang
	*/
	FAN("Fang", "fan"),
	/**
	* Faroese
	*/
	FO("Faroese", "fo", "fao"),
	/**
	* Fanti
	*/
	FAT("Fanti", "fat"),
	/**
	* Fijian
	*/
	FJ("Fijian", "fj", "fij"),
	/**
	* Filipino; Pilipino
	*/
	FIL("Filipino; Pilipino", "fil"),
	/**
	* Finnish
	*/
	FI("Finnish", "fi", "fin"),
	/**
	* Finno-Ugrian languages
	*/
	FIU("Finno-Ugrian languages", "fiu"),
	/**
	* Fon
	*/
	FON("Fon", "fon"),
	/**
	* French
	*/
	FR("French", "fr", "fre", "fra"),
	/**
	* French, Middle (ca.1400-1600)
	*/
	FRM("French, Middle (ca.1400-1600)", "frm"),
	/**
	* French, Old (842-ca.1400)
	*/
	FRO("French, Old (842-ca.1400)", "fro"),
	/**
	* Northern Frisian
	*/
	FRR("Northern Frisian", "frr"),
	/**
	* Eastern Frisian
	*/
	FRS("Eastern Frisian", "frs"),
	/**
	* Western Frisian
	*/
	FY("Western Frisian", "fy", "fry"),
	/**
	* Fulah
	*/
	FF("Fulah", "ff", "ful"),
	/**
	* Friulian
	*/
	FUR("Friulian", "fur"),
	/**
	* Ga
	*/
	GAA("Ga", "gaa"),
	/**
	* Gayo
	*/
	GAY("Gayo", "gay"),
	/**
	* Gbaya
	*/
	GBA("Gbaya", "gba"),
	/**
	* Germanic languages
	*/
	GEM("Germanic languages", "gem"),
	/**
	* Georgian
	*/
	KA("Georgian", "ka", "geo", "kat"),
	/**
	* German
	*/
	DE("German", "de", "ger", "deu"),
	/**
	* Geez
	*/
	GEZ("Geez", "gez"),
	/**
	* Gilbertese
	*/
	GIL("Gilbertese", "gil"),
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
	* German, Middle High (ca.1050-1500)
	*/
	GMH("German, Middle High (ca.1050-1500)", "gmh"),
	/**
	* German, Old High (ca.750-1050)
	*/
	GOH("German, Old High (ca.750-1050)", "goh"),
	/**
	* Gondi
	*/
	GON("Gondi", "gon"),
	/**
	* Gorontalo
	*/
	GOR("Gorontalo", "gor"),
	/**
	* Gothic
	*/
	GOT("Gothic", "got"),
	/**
	* Grebo
	*/
	GRB("Grebo", "grb"),
	/**
	* Greek, Ancient (to 1453)
	*/
	GRC("Greek, Ancient (to 1453)", "grc"),
	/**
	* Greek, Modern (1453-)
	*/
	EL("Greek, Modern (1453-)", "el", "gre", "ell"),
	/**
	* Guarani
	*/
	GN("Guarani", "gn", "grn"),
	/**
	* Swiss German; Alemannic; Alsatian
	*/
	GSW("Swiss German; Alemannic; Alsatian", "gsw"),
	/**
	* Gujarati
	*/
	GU("Gujarati", "gu", "guj"),
	/**
	* Gwich'in
	*/
	GWI("Gwich'in", "gwi"),
	/**
	* Haida
	*/
	HAI("Haida", "hai"),
	/**
	* Haitian; Haitian Creole
	*/
	HT("Haitian; Haitian Creole", "ht", "hat"),
	/**
	* Hausa
	*/
	HA("Hausa", "ha", "hau"),
	/**
	* Hawaiian
	*/
	HAW("Hawaiian", "haw"),
	/**
	* Hebrew
	*/
	HE("Hebrew", "he", "heb"),
	/**
	* Herero
	*/
	HZ("Herero", "hz", "her"),
	/**
	* Hiligaynon
	*/
	HIL("Hiligaynon", "hil"),
	/**
	* Himachali languages; Western Pahari languages
	*/
	HIM("Himachali languages; Western Pahari languages", "him"),
	/**
	* Hindi
	*/
	HI("Hindi", "hi", "hin"),
	/**
	* Hittite
	*/
	HIT("Hittite", "hit"),
	/**
	* Hmong; Mong
	*/
	HMN("Hmong; Mong", "hmn"),
	/**
	* Hiri Motu
	*/
	HO("Hiri Motu", "ho", "hmo"),
	/**
	* Croatian
	*/
	HR("Croatian", "hr", "hrv"),
	/**
	* Upper Sorbian
	*/
	HSB("Upper Sorbian", "hsb"),
	/**
	* Hungarian
	*/
	HU("Hungarian", "hu", "hun"),
	/**
	* Hupa
	*/
	HUP("Hupa", "hup"),
	/**
	* Iban
	*/
	IBA("Iban", "iba"),
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
	* Ijo languages
	*/
	IJO("Ijo languages", "ijo"),
	/**
	* Inuktitut
	*/
	IU("Inuktitut", "iu", "iku"),
	/**
	* Interlingue; Occidental
	*/
	IE("Interlingue; Occidental", "ie", "ile"),
	/**
	* Iloko
	*/
	ILO("Iloko", "ilo"),
	/**
	* Interlingua (International Auxiliary Language Association)
	*/
	IA("Interlingua (International Auxiliary Language Association)", "ia", "ina"),
	/**
	* Indic languages
	*/
	INC("Indic languages", "inc"),
	/**
	* Indonesian
	*/
	ID("Indonesian", "id", "ind"),
	/**
	* Indo-European languages
	*/
	INE("Indo-European languages", "ine"),
	/**
	* Ingush
	*/
	INH("Ingush", "inh"),
	/**
	* Inupiaq
	*/
	IK("Inupiaq", "ik", "ipk"),
	/**
	* Iranian languages
	*/
	IRA("Iranian languages", "ira"),
	/**
	* Iroquoian languages
	*/
	IRO("Iroquoian languages", "iro"),
	/**
	* Italian
	*/
	IT("Italian", "it", "ita"),
	/**
	* Javanese
	*/
	JV("Javanese", "jv", "jav"),
	/**
	* Lojban
	*/
	JBO("Lojban", "jbo"),
	/**
	* Japanese
	*/
	JA("Japanese", "ja", "jpn"),
	/**
	* Judeo-Persian
	*/
	JPR("Judeo-Persian", "jpr"),
	/**
	* Judeo-Arabic
	*/
	JRB("Judeo-Arabic", "jrb"),
	/**
	* Kara-Kalpak
	*/
	KAA("Kara-Kalpak", "kaa"),
	/**
	* Kabyle
	*/
	KAB("Kabyle", "kab"),
	/**
	* Kachin; Jingpho
	*/
	KAC("Kachin; Jingpho", "kac"),
	/**
	* Kalaallisut; Greenlandic
	*/
	KL("Kalaallisut; Greenlandic", "kl", "kal"),
	/**
	* Kamba
	*/
	KAM("Kamba", "kam"),
	/**
	* Kannada
	*/
	KN("Kannada", "kn", "kan"),
	/**
	* Karen languages
	*/
	KAR("Karen languages", "kar"),
	/**
	* Kashmiri
	*/
	KS("Kashmiri", "ks", "kas"),
	/**
	* Kanuri
	*/
	KR("Kanuri", "kr", "kau"),
	/**
	* Kawi
	*/
	KAW("Kawi", "kaw"),
	/**
	* Kazakh
	*/
	KK("Kazakh", "kk", "kaz"),
	/**
	* Kabardian
	*/
	KBD("Kabardian", "kbd"),
	/**
	* Khasi
	*/
	KHA("Khasi", "kha"),
	/**
	* Khoisan languages
	*/
	KHI("Khoisan languages", "khi"),
	/**
	* Central Khmer
	*/
	KM("Central Khmer", "km", "khm"),
	/**
	* Khotanese; Sakan
	*/
	KHO("Khotanese; Sakan", "kho"),
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
	* Kimbundu
	*/
	KMB("Kimbundu", "kmb"),
	/**
	* Konkani
	*/
	KOK("Konkani", "kok"),
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
	* Kosraean
	*/
	KOS("Kosraean", "kos"),
	/**
	* Kpelle
	*/
	KPE("Kpelle", "kpe"),
	/**
	* Karachay-Balkar
	*/
	KRC("Karachay-Balkar", "krc"),
	/**
	* Karelian
	*/
	KRL("Karelian", "krl"),
	/**
	* Kru languages
	*/
	KRO("Kru languages", "kro"),
	/**
	* Kurukh
	*/
	KRU("Kurukh", "kru"),
	/**
	* Kuanyama; Kwanyama
	*/
	KJ("Kuanyama; Kwanyama", "kj", "kua"),
	/**
	* Kumyk
	*/
	KUM("Kumyk", "kum"),
	/**
	* Kurdish
	*/
	KU("Kurdish", "ku", "kur"),
	/**
	* Kutenai
	*/
	KUT("Kutenai", "kut"),
	/**
	* Ladino
	*/
	LAD("Ladino", "lad"),
	/**
	* Lahnda
	*/
	LAH("Lahnda", "lah"),
	/**
	* Lamba
	*/
	LAM("Lamba", "lam"),
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
	* Lezghian
	*/
	LEZ("Lezghian", "lez"),
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
	* Mongo
	*/
	LOL("Mongo", "lol"),
	/**
	* Lozi
	*/
	LOZ("Lozi", "loz"),
	/**
	* Luxembourgish; Letzeburgesch
	*/
	LB("Luxembourgish; Letzeburgesch", "lb", "ltz"),
	/**
	* Luba-Lulua
	*/
	LUA("Luba-Lulua", "lua"),
	/**
	* Luba-Katanga
	*/
	LU("Luba-Katanga", "lu", "lub"),
	/**
	* Ganda
	*/
	LG("Ganda", "lg", "lug"),
	/**
	* Luiseno
	*/
	LUI("Luiseno", "lui"),
	/**
	* Lunda
	*/
	LUN("Lunda", "lun"),
	/**
	* Luo (Kenya and Tanzania)
	*/
	LUO("Luo (Kenya and Tanzania)", "luo"),
	/**
	* Lushai
	*/
	LUS("Lushai", "lus"),
	/**
	* Macedonian
	*/
	MK("Macedonian", "mk", "mac", "mkd"),
	/**
	* Madurese
	*/
	MAD("Madurese", "mad"),
	/**
	* Magahi
	*/
	MAG("Magahi", "mag"),
	/**
	* Marshallese
	*/
	MH("Marshallese", "mh", "mah"),
	/**
	* Maithili
	*/
	MAI("Maithili", "mai"),
	/**
	* Makasar
	*/
	MAK("Makasar", "mak"),
	/**
	* Malayalam
	*/
	ML("Malayalam", "ml", "mal"),
	/**
	* Mandingo
	*/
	MAN("Mandingo", "man"),
	/**
	* Maori
	*/
	MI("Maori", "mi", "mao", "mri"),
	/**
	* Austronesian languages
	*/
	MAP("Austronesian languages", "map"),
	/**
	* Marathi
	*/
	MR("Marathi", "mr", "mar"),
	/**
	* Masai
	*/
	MAS("Masai", "mas"),
	/**
	* Malay
	*/
	MS("Malay", "ms", "may", "msa"),
	/**
	* Moksha
	*/
	MDF("Moksha", "mdf"),
	/**
	* Mandar
	*/
	MDR("Mandar", "mdr"),
	/**
	* Mende
	*/
	MEN("Mende", "men"),
	/**
	* Irish, Middle (900-1200)
	*/
	MGA("Irish, Middle (900-1200)", "mga"),
	/**
	* Mi'kmaq; Micmac
	*/
	MIC("Mi'kmaq; Micmac", "mic"),
	/**
	* Minangkabau
	*/
	MIN("Minangkabau", "min"),
	/**
	* Uncoded languages
	*/
	MIS("Uncoded languages", "mis"),
	/**
	* Mon-Khmer languages
	*/
	MKH("Mon-Khmer languages", "mkh"),
	/**
	* Malagasy
	*/
	MG("Malagasy", "mg", "mlg"),
	/**
	* Maltese
	*/
	MT("Maltese", "mt", "mlt"),
	/**
	* Manchu
	*/
	MNC("Manchu", "mnc"),
	/**
	* Manipuri
	*/
	MNI("Manipuri", "mni"),
	/**
	* Manobo languages
	*/
	MNO("Manobo languages", "mno"),
	/**
	* Mohawk
	*/
	MOH("Mohawk", "moh"),
	/**
	* Mongolian
	*/
	MN("Mongolian", "mn", "mon"),
	/**
	* Mossi
	*/
	MOS("Mossi", "mos"),
	/**
	* Multiple languages
	*/
	MUL("Multiple languages", "mul"){
		@Override
		public boolean matches(String code) {
			return true;
		}
	},
	/**
	* Munda languages
	*/
	MUN("Munda languages", "mun"),
	/**
	* Creek
	*/
	MUS("Creek", "mus"),
	/**
	* Mirandese
	*/
	MWL("Mirandese", "mwl"),
	/**
	* Marwari
	*/
	MWR("Marwari", "mwr"),
	/**
	* Mayan languages
	*/
	MYN("Mayan languages", "myn"),
	/**
	* Erzya
	*/
	MYV("Erzya", "myv"),
	/**
	* Nahuatl languages
	*/
	NAH("Nahuatl languages", "nah"),
	/**
	* North American Indian languages
	*/
	NAI("North American Indian languages", "nai"),
	/**
	* Neapolitan
	*/
	NAP("Neapolitan", "nap"),
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
	* Low German; Low Saxon; German, Low; Saxon, Low
	*/
	NDS("Low German; Low Saxon; German, Low; Saxon, Low", "nds"),
	/**
	* Nepali
	*/
	NE("Nepali", "ne", "nep"),
	/**
	* Nepal Bhasa; Newari
	*/
	NEW("Nepal Bhasa; Newari", "new"),
	/**
	* Nias
	*/
	NIA("Nias", "nia"),
	/**
	* Niger-Kordofanian languages
	*/
	NIC("Niger-Kordofanian languages", "nic"),
	/**
	* Niuean
	*/
	NIU("Niuean", "niu"),
	/**
	* Norwegian Nynorsk; Nynorsk, Norwegian
	*/
	NN("Norwegian Nynorsk; Nynorsk, Norwegian", "nn", "nno"),
	/**
	* Bokmål, Norwegian; Norwegian Bokmål
	*/
	NB("Bokmål, Norwegian; Norwegian Bokmål", "nb", "nob"),
	/**
	* Nogai
	*/
	NOG("Nogai", "nog"),
	/**
	* Norse, Old
	*/
	NON("Norse, Old", "non"),
	/**
	* Norwegian
	*/
	NO("Norwegian", "no", "nor"),
	/**
	* N'Ko
	*/
	NQO("N'Ko", "nqo"),
	/**
	* Pedi; Sepedi; Northern Sotho
	*/
	NSO("Pedi; Sepedi; Northern Sotho", "nso"),
	/**
	* Nubian languages
	*/
	NUB("Nubian languages", "nub"),
	/**
	* Classical Newari; Old Newari; Classical Nepal Bhasa
	*/
	NWC("Classical Newari; Old Newari; Classical Nepal Bhasa", "nwc"),
	/**
	* Chichewa; Chewa; Nyanja
	*/
	NY("Chichewa; Chewa; Nyanja", "ny", "nya"),
	/**
	* Nyamwezi
	*/
	NYM("Nyamwezi", "nym"),
	/**
	* Nyankole
	*/
	NYN("Nyankole", "nyn"),
	/**
	* Nyoro
	*/
	NYO("Nyoro", "nyo"),
	/**
	* Nzima
	*/
	NZI("Nzima", "nzi"),
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
	* Osage
	*/
	OSA("Osage", "osa"),
	/**
	* Ossetian; Ossetic
	*/
	OS("Ossetian; Ossetic", "os", "oss"),
	/**
	* Turkish, Ottoman (1500-1928)
	*/
	OTA("Turkish, Ottoman (1500-1928)", "ota"),
	/**
	* Otomian languages
	*/
	OTO("Otomian languages", "oto"),
	/**
	* Papuan languages
	*/
	PAA("Papuan languages", "paa"),
	/**
	* Pangasinan
	*/
	PAG("Pangasinan", "pag"),
	/**
	* Pahlavi
	*/
	PAL("Pahlavi", "pal"),
	/**
	* Pampanga; Kapampangan
	*/
	PAM("Pampanga; Kapampangan", "pam"),
	/**
	* Panjabi; Punjabi
	*/
	PA("Panjabi; Punjabi", "pa", "pan"),
	/**
	* Papiamento
	*/
	PAP("Papiamento", "pap"),
	/**
	* Palauan
	*/
	PAU("Palauan", "pau"),
	/**
	* Persian, Old (ca.600-400 B.C.)
	*/
	PEO("Persian, Old (ca.600-400 B.C.)", "peo"),
	/**
	* Persian
	*/
	FA("Persian", "fa", "per", "fas"),
	/**
	* Philippine languages
	*/
	PHI("Philippine languages", "phi"),
	/**
	* Phoenician
	*/
	PHN("Phoenician", "phn"),
	/**
	* Pali
	*/
	PI("Pali", "pi", "pli"),
	/**
	* Polish
	*/
	PL("Polish", "pl", "pol"),
	/**
	* Pohnpeian
	*/
	PON("Pohnpeian", "pon"),
	/**
	* Portuguese
	*/
	PT("Portuguese", "pt", "por"),
	/**
	* Prakrit languages
	*/
	PRA("Prakrit languages", "pra"),
	/**
	* Provençal, Old (to 1500)
	*/
	PRO("Provençal, Old (to 1500)", "pro"),
	/**
	* Pushto; Pashto
	*/
	PS("Pushto; Pashto", "ps", "pus"),
	/**
	* Reserved for local use
	*/
	QAA_QTZ("Reserved for local use", "qaa-qtz"),
	/**
	* Quechua
	*/
	QU("Quechua", "qu", "que"),
	/**
	* Rajasthani
	*/
	RAJ("Rajasthani", "raj"),
	/**
	* Rapanui
	*/
	RAP("Rapanui", "rap"),
	/**
	* Rarotongan; Cook Islands Maori
	*/
	RAR("Rarotongan; Cook Islands Maori", "rar"),
	/**
	* Romance languages
	*/
	ROA("Romance languages", "roa"),
	/**
	* Romansh
	*/
	RM("Romansh", "rm", "roh"),
	/**
	* Romany
	*/
	ROM("Romany", "rom"),
	/**
	* Romanian; Moldavian; Moldovan
	*/
	RO("Romanian; Moldavian; Moldovan", "ro", "rum", "ron"),
	/**
	* Romanian; Moldavian; Moldovan
	*/
	MO("Moldavian", "mo", "mol", "Moldovan"),
	/**
	* Rundi
	*/
	RN("Rundi", "rn", "run"),
	/**
	* Aromanian; Arumanian; Macedo-Romanian
	*/
	RUP("Aromanian; Arumanian; Macedo-Romanian", "rup"),
	/**
	* Russian
	*/
	RU("Russian", "ru", "rus"),
	/**
	* Sandawe
	*/
	SAD("Sandawe", "sad"),
	/**
	* Sango
	*/
	SG("Sango", "sg", "sag"),
	/**
	* Yakut
	*/
	SAH("Yakut", "sah"),
	/**
	* South American Indian (Other)
	*/
	SAI("South American Indian (Other)", "sai"),
	/**
	* Salishan languages
	*/
	SAL("Salishan languages", "sal"),
	/**
	* Samaritan Aramaic
	*/
	SAM("Samaritan Aramaic", "sam"),
	/**
	* Sanskrit
	*/
	SA("Sanskrit", "sa", "san"),
	/**
	* Sasak
	*/
	SAS("Sasak", "sas"),
	/**
	* Santali
	*/
	SAT("Santali", "sat"),
	/**
	* Sicilian
	*/
	SCN("Sicilian", "scn"),
	/**
	* Scots
	*/
	SCO("Scots", "sco"),
	/**
	* Selkup
	*/
	SEL("Selkup", "sel"),
	/**
	* Semitic languages
	*/
	SEM("Semitic languages", "sem"),
	/**
	* Irish, Old (to 900)
	*/
	SGA("Irish, Old (to 900)", "sga"),
	/**
	* Sign Languages
	*/
	SGN("Sign Languages", "sgn"),
	/**
	* Shan
	*/
	SHN("Shan", "shn"),
	/**
	* Sidamo
	*/
	SID("Sidamo", "sid"),
	/**
	* Sinhala; Sinhalese
	*/
	SI("Sinhala; Sinhalese", "si", "sin"),
	/**
	* Siouan languages
	*/
	SIO("Siouan languages", "sio"),
	/**
	* Sino-Tibetan languages
	*/
	SIT("Sino-Tibetan languages", "sit"),
	/**
	* Slavic languages
	*/
	SLA("Slavic languages", "sla"),
	/**
	* Slovak
	*/
	SK("Slovak", "sk", "slo", "slk"),
	/**
	* Slovenian
	*/
	SL("Slovenian", "sl", "slv"),
	/**
	* Southern Sami
	*/
	SMA("Southern Sami", "sma"),
	/**
	* Northern Sami
	*/
	SE("Northern Sami", "se", "sme"),
	/**
	* Sami languages
	*/
	SMI("Sami languages", "smi"),
	/**
	* Lule Sami
	*/
	SMJ("Lule Sami", "smj"),
	/**
	* Inari Sami
	*/
	SMN("Inari Sami", "smn"),
	/**
	* Samoan
	*/
	SM("Samoan", "sm", "smo"),
	/**
	* Skolt Sami
	*/
	SMS("Skolt Sami", "sms"),
	/**
	* Shona
	*/
	SN("Shona", "sn", "sna"),
	/**
	* Sindhi
	*/
	SD("Sindhi", "sd", "snd"),
	/**
	* Soninke
	*/
	SNK("Soninke", "snk"),
	/**
	* Sogdian
	*/
	SOG("Sogdian", "sog"),
	/**
	* Somali
	*/
	SO("Somali", "so", "som"),
	/**
	* Songhai languages
	*/
	SON("Songhai languages", "son"),
	/**
	* Sotho, Southern
	*/
	ST("Sotho, Southern", "st", "sot"),
	/**
	* Spanish; Castilian
	*/
	ES("Spanish", "es", "spa","Castilian"),
	/**
	* Sardinian
	*/
	SC("Sardinian", "sc", "srd"),
	/**
	* Sranan Tongo
	*/
	SRN("Sranan Tongo", "srn"),
	/**
	* Serbian
	*/
	SR("Serbian", "sr", "srp"),
	/**
	*  Serbo-Croatian
	*/
	@Deprecated
	SH("Serbo-Croatian", "sh"),
	/**
	* Serer
	*/
	SRR("Serer", "srr"),
	/**
	* Nilo-Saharan languages
	*/
	SSA("Nilo-Saharan languages", "ssa"),
	/**
	* Swati
	*/
	SS("Swati", "ss", "ssw"),
	/**
	* Sukuma
	*/
	SUK("Sukuma", "suk"),
	/**
	* Sundanese
	*/
	SU("Sundanese", "su", "sun"),
	/**
	* Susu
	*/
	SUS("Susu", "sus"),
	/**
	* Sumerian
	*/
	SUX("Sumerian", "sux"),
	/**
	* Swahili
	*/
	SW("Swahili", "sw", "swa"),
	/**
	* Swedish
	*/
	SV("Swedish", "sv", "swe"),
	/**
	* Classical Syriac
	*/
	SYC("Classical Syriac", "syc"),
	/**
	* Syriac
	*/
	SYR("Syriac", "syr"),
	/**
	* Tahitian
	*/
	TY("Tahitian", "ty", "tah"),
	/**
	* Tai languages
	*/
	TAI("Tai languages", "tai"),
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
	* Timne
	*/
	TEM("Timne", "tem"),
	/**
	* Tereno
	*/
	TER("Tereno", "ter"),
	/**
	* Tetum
	*/
	TET("Tetum", "tet"),
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
	* Tigre
	*/
	TIG("Tigre", "tig"),
	/**
	* Tigrinya
	*/
	TI("Tigrinya", "ti", "tir"),
	/**
	* Tiv
	*/
	TIV("Tiv", "tiv"),
	/**
	* Tokelau
	*/
	TKL("Tokelau", "tkl"),
	/**
	* Klingon; tlhIngan-Hol
	*/
	TLH("Klingon; tlhIngan-Hol", "tlh"),
	/**
	* Tlingit
	*/
	TLI("Tlingit", "tli"),
	/**
	* Tamashek
	*/
	TMH("Tamashek", "tmh"),
	/**
	* Tonga (Nyasa)
	*/
	TOG("Tonga (Nyasa)", "tog"),
	/**
	* Tonga (Tonga Islands)
	*/
	TO("Tonga (Tonga Islands)", "to", "ton"),
	/**
	* Tok Pisin
	*/
	TPI("Tok Pisin", "tpi"),
	/**
	* Tsimshian
	*/
	TSI("Tsimshian", "tsi"),
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
	* Tumbuka
	*/
	TUM("Tumbuka", "tum"),
	/**
	* Tupi languages
	*/
	TUP("Tupi languages", "tup"),
	/**
	* Turkish
	*/
	TR("Turkish", "tr", "tur"),
	/**
	* Altaic languages
	*/
	TUT("Altaic languages", "tut"),
	/**
	* Tuvalu
	*/
	TVL("Tuvalu", "tvl"),
	/**
	* Twi
	*/
	TW("Twi", "tw", "twi"),
	/**
	* Tuvinian
	*/
	TYV("Tuvinian", "tyv"),
	/**
	* Udmurt
	*/
	UDM("Udmurt", "udm"),
	/**
	* Ugaritic
	*/
	UGA("Ugaritic", "uga"),
	/**
	* Uighur; Uyghur
	*/
	UG("Uighur; Uyghur", "ug", "uig"),
	/**
	* Ukrainian
	*/
	UK("Ukrainian", "uk", "ukr"),
	/**
	* Umbundu
	*/
	UMB("Umbundu", "umb"),
	/**
	* Undetermined
	*/
	UND("Undetermined", "und"),
	/**
	* Urdu
	*/
	UR("Urdu", "ur", "urd"),
	/**
	* Uzbek
	*/
	UZ("Uzbek", "uz", "uzb"),
	/**
	* Vai
	*/
	VAI("Vai", "vai"),
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
	* Votic
	*/
	VOT("Votic", "vot"),
	/**
	* Wakashan languages
	*/
	WAK("Wakashan languages", "wak"),
	/**
	* Walamo
	*/
	WAL("Walamo", "wal"),
	/**
	* Waray
	*/
	WAR("Waray", "war"),
	/**
	* Washo
	*/
	WAS("Washo", "was"),
	/**
	* Welsh
	*/
	CY("Welsh", "cy", "wel", "cym"),
	/**
	* Sorbian languages
	*/
	WEN("Sorbian languages", "wen"),
	/**
	* Walloon
	*/
	WA("Walloon", "wa", "wln"),
	/**
	* Wolof
	*/
	WO("Wolof", "wo", "wol"),
	/**
	* Kalmyk; Oirat
	*/
	XAL("Kalmyk; Oirat", "xal"),
	/**
	* Xhosa
	*/
	XH("Xhosa", "xh", "xho"),
	/**
	* Yao
	*/
	YAO("Yao", "yao"),
	/**
	* Yapese
	*/
	YAP("Yapese", "yap"),
	/**
	* Yiddish
	*/
	YI("Yiddish", "yi", "yid"),
	/**
	* Yoruba
	*/
	YO("Yoruba", "yo", "yor"),
	/**
	* Yupik languages
	*/
	YPK("Yupik languages", "ypk"),
	/**
	* Zapotec
	*/
	ZAP("Zapotec", "zap"),
	/**
	* Blissymbols; Blissymbolics; Bliss
	*/
	ZBL("Blissymbols; Blissymbolics; Bliss", "zbl"),
	/**
	* Zenaga
	*/
	ZEN("Zenaga", "zen"),
	/**
	* Standard Moroccan Tamazight
	*/
	ZGH("Standard Moroccan Tamazight", "zgh"),
	/**
	* Zhuang; Chuang
	*/
	ZA("Zhuang; Chuang", "za", "zha"),
	/**
	* Zande languages
	*/
	ZND("Zande languages", "znd"),
	/**
	* Zulu
	*/
	ZU("Zulu", "zu", "zul"),
	/**
	* Zuni
	*/
	ZUN("Zuni", "zun"),
	/**
	* No linguistic content; Not applicable
	*/
	ZXX("No linguistic content; Not applicable", "zxx"),
	/**
	* Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki
	*/
	ZZA("Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki", "zza"),



	DEFAULT("Default", "default", "def"),
	
	UNKNOWN("Unknown", "unknown","un", "unk");

	
	
	
	private String name;
	private String[] codes;
	
	private Language(String name, String... codes) {
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

	public static Language getLanguage(String code){
		for (Language lang : Language.values()) {
			if (lang.belongsTo(code) || match(code, lang))
				return lang;
		}
		Logger.warn("unkown language "+code);
		return UNKNOWN;
	}

	private static boolean match(String code, Language lang) {
		return lang.name.toLowerCase().equals(code.toLowerCase());
	}

	public static boolean isLanguage(String code) {
		for (Language lang : Language.values()) {
			if (lang.belongsTo(code))
				return true;
		}
		return false;
	}

	/**
	 * tells if the code is one of the codes to refer to this language.
	 * @param code
	 * @return true iff the code is used to refer to this language.
	 */
	public boolean belongsTo(String code) {
		if (Utils.isValidURL(code)){
			int l = code.lastIndexOf("/");
			code = code.substring(l+1);
		}
		for (String langcode: this.getCodes()) {
			if (code.toLowerCase().equals(langcode))
				return true;
		}
		return false;
	}
	
	public boolean matches(String code) {
		return belongsTo(code);
	}

}
