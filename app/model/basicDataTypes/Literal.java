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

import java.util.HashMap;
import java.util.Locale;

import org.mongodb.morphia.annotations.Embedded;

@Embedded
public class Literal extends HashMap<String, String> {

	public static enum Language {
		ABK,ACE,ACH,ADA,ADY,AAR,AFH,AFR,AFA,AIN,AKA,AKK,AQA,ALB,ALE,AQL,ALG,TUT,AMH,GRC,ANP,
		APA,ARA,ARG,ARP,AUF,ARW,AWD,ARM,HYX,ART,ASM,AST,ATH,ALV,AUS,AAV,MAP,AVA,AVE,AWA,AYM,AZE,
		BAN,BAT,BAL,BAM,BAI,BAD,BNT,BAS,BAK,BAQ,EUQ,BTK,BEJ,BEL,BEM,BEN,BER,BHO,BIH,BIK,BYN,BIN,BIS,ZBL,
		BOS,BRA,BRE,BUG,BUL,BUA,BUR,CAD,CDD,CAT,CAU,CEB,CEL,CAI,KHM,PLF,CSU,CDC,CHG,CMC,CHA,CHE,CHR,
		CHY,CHB,CBA,CHI,ZHX,CHN,CHP,CHO,CHU,CHK,CHV,NWC,SYC,COP,COR,COS,CRE,MUS,CRP,CRH,HRV,CUS,CES,DAK,
		DAN,DAR,DEL,DIV,DIN,DOI,DGR,DRA,DUA,DUT,DYU,DZO,GME,ZLE,FRS,PQE,SDV,EFI,EGY,EGX,EKA,ELX,ENG,CPE,
		MYV,ESX,EPO,EST,EWE,EWO,FAN,FAT,FAO,FIJ,FIL,FIN,FIU,FON,FOX,FRA,CPF,FUR,FUL,GAA,CAR,GLG,LUG,GAY,
		GBA,GEZ,GEO,DEU,GEM,GIL,GON,GOR,GOT,GRB,GRK,GRN,GUJ,GWI,HAI,HAT,HAU,HAW,HEB,HER,HIL,HIM,HIN,HMO,HIT,
		HMN,HMX,HOK,HUN,HUP,IBA,ICE,IDO,IBO,IJO,ILO,SMN,INC,INE,IIR,IND,INH,INA,ILE,IKU,IPK,IRA,GLE,IRO,ITA,ITC,
		JPN,JPX,JAV,JRB,JPR,KBD,KAB,KAC,KAL,XAL,KAM,KAN,KAU,KAA,KRC,KRL,KAR,KAS,CSB,KAW,KAZ,KHA,KHI,KHO,KIK,KMB,
		KIN,KIR,TLH,KOM,KON,KOK,KDO,KOR,KOS,KPE,KRO,KUA,KUM,KUR,KRU,KUT,LAD,LAH,LAM,DAY,LAO,LAT,LAV,LEZ,LIM,LIN,
		LIT,JBO,NDS,DSB,LOZ,LUB,LUA,LUI,SMJ,LUN,LUO,LUS,LTZ,RUP,MAC,MAD,MAG,MAI,MAK,MLG,MAY,MAL,POZ,MLT,MNC,MDR,
		DMN,MAN,MNI,MNO,GLV,MAO,ARN,MAR,CHM,MAH,MWR,MAS,MYN,MEN,MIC,DUM,ENM,FRM,GMH,MGA,MIN,MWL,GRE,MOH,MDF,MKH,LOL,MON,
		XGN,MOS,MUL,MUN,NQO,XND,NAH,NAU,NAV,NDO,NAP,NEW,NEP,NIA,NIC,SSA,NIU,ZXX,NOG,NAI,CCN,GMQ,NDE,FRR,SME,NOR,NOB,NNO,
		NUB,NYM,NYA,NYN,NYO,NZI,OCI,ORI,ARC,OJI,ANG,FRO,GOH,SGA,NON,PEO,PRO,OMV,ORM,OSA,OSS,OMQ,OTO,OTA,PAL,PAU,PLI,PAM,
		PAG,PAN,PAP,PAA,NSO,FAS,PHI,PHN,PON,POL,POR,CPP,PRA,PUS,QUE,QWE,RAJ,RAP,RAR,ROA,RON,ROH,ROM,RUN,RUS,SAL,SAM,SMI,
		SMO,SYD,SAD,SAG,SAN,SAT,SRD,SAS,SCO,GLA,SEL,SEM,SRP,SRR,SHN,SNA,III,SCN,SID,SGN,BLA,SND,SIN,SIT,SIO,SMS,DEN,SLA,SLK,
		SLV,SOG,SOM,SON,SNK,WEN,SAI,CCS,NBL,ZLS,ALT,SMA,SOT,SPA,SRN,ZGH,SUK,SUX,SUN,SUS,SWA,SSW,SWE,GSW,SYR,TGL,TAH,TAI,TGK,TMH,
		TAM,TAT,TEL,TER,TET,THA,BOD,TBQ,TIG,TIR,TEM,TIV,TLI,TPI,TKL,TOG,TON,NGF,TSI,TSO,TSN,TUM,TUW,TUP,TRK,TUR,TUK,TVL,TYV,TWI,
		UDM,UGA,UIG,UKR,UMB,MIS,UND,HSB,URJ,URD,AZC,UZB,VAI,VEN,VIE,VOL,VOT,WAK,WLN,WAR,WAS,CYM,GMW,ZLW,FRY,PQW,WAL,WOL,XHO,SAH,YAO,
		YAP,YID,YOR,YPK,ZND,ZAP,ZZA,ZEN,ZHA,ZUL,ZUN,UNKNOWN, DEF;
		
		public String toString() {
	        return name().toLowerCase();
	    }
		
		
		//use this method instead of valueOf
		public Language getLanguage(String str) {
			for (Language lang : Language.values()) {
	            if (lang.toString().equals(str.toUpperCase())) {
	                return lang;
	            }
	        }
	        return null;
		}
		
		/*String fullName;
		
		public Language(String fullName) {
			this.fullName = fullName;
		}*/
	}

	public Literal() {
	}

	public Literal(String label) {
		this.put(Language.UNKNOWN.toString(), label);
	}

	public Literal(Language lang, String label) {
		this.put(lang.toString(), label);
		if (lang.equals(Language.ENG))
			this.put(Language.DEF.toString(), label);
	}

	public Literal(String lang, String label) {
		this.put(lang, label);
	}

	// keys are language 2 letter codes,
	// "unknown" for unknown language
	public void setLiteral(Language lang, String label) {
		put(lang.toString(), label);
	}
	/**
	 * Don't request the "unknown" language, request "any" if you don't care
		 * @param lang
	 * @return
	 */
	public String getLiteral(Language lang) {
		/*if(Language.ANY.equals(lang)) {
			return this.get(this.keySet().toArray()[0]);
		}
		else*/
			return get(lang.toString());
	}
}
