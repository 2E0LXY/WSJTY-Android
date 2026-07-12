package uk.co.wsjty.remote.data

/**
 * Maps an amateur callsign to a country flag emoji via ITU/DXCC prefix
 * rules, longest-prefix-first. Covers the common DXCC entities; unknown
 * prefixes return null (no flag shown).
 */
object CallsignFlags {

    /** Sender's flag for a decoded FT8/FT4 message, or null. */
    fun flagForMessage(message: String): String? {
        val call = senderCallsign(message) ?: return null
        return flagForCall(call)
    }

    /**
     * FT8 standard messages: "CQ [DIR] SENDER GRID", "TO SENDER RPT",
     * "TO SENDER RR73", etc. The sender is the second callsign, or the
     * token after CQ (skipping 1-4 letter directional like DX/EU/NA/POTA).
     */
    fun senderCallsign(message: String): String? {
        val t = message.trim().split(Regex("\\s+"))
        if (t.isEmpty()) return null
        return if (t[0].equals("CQ", true)) {
            val cand = t.drop(1).firstOrNull { looksLikeCall(it) }
            cand
        } else {
            t.getOrNull(1)?.takeIf { looksLikeCall(it) }
        }
    }

    private fun looksLikeCall(s: String): Boolean {
        val core = s.trim('<', '>')
        if (core.length < 3 || core.length > 12) return false
        return core.any { it.isDigit() } && core.any { it.isLetter() } &&
            core.all { it.isLetterOrDigit() || it == '/' }
    }

    fun flagForCall(rawCall: String): String? {
        var call = rawCall.trim('<', '>').uppercase()
        if ('/' in call) {
            // Prefix-form portable e.g. EA8/G4ABC — use the shorter side if
            // it looks like a pure prefix, else the longer side's start.
            val parts = call.split('/')
            call = parts.minByOrNull { it.length } ?: call
        }
        var len = minOf(4, call.length)
        while (len > 0) {
            prefixes[call.substring(0, len)]?.let { return emoji(it) }
            len--
        }
        return null
    }

    private fun emoji(iso: String): String =
        iso.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')) }
            .joinToString("") { String(it) }

    // ISO 3166 alpha-2 per prefix. Longest match wins.
    private val prefixes: Map<String, String> = buildMap {
        // United Kingdom & crown dependencies
        listOf("G", "M", "2E", "2M", "2W", "2I", "2D", "2J", "2U").forEach { put(it, "GB") }
        put("GM", "GB"); put("MM", "GB"); put("GW", "GB"); put("MW", "GB")
        put("GI", "GB"); put("MI", "GB"); put("GD", "IM"); put("GU", "GG"); put("GJ", "JE")
        // Europe
        listOf("DL", "DA", "DB", "DC", "DD", "DF", "DG", "DH", "DJ", "DK", "DM", "DN", "DO", "DP", "DR").forEach { put(it, "DE") }
        listOf("F").forEach { put(it, "FR") }
        listOf("EA", "EB", "EC", "ED", "EE", "EF", "EG", "EH", "AM", "AN", "AO").forEach { put(it, "ES") }
        listOf("I").forEach { put(it, "IT") }
        listOf("CT", "CQ", "CR", "CS").forEach { put(it, "PT") }
        listOf("PA", "PB", "PC", "PD", "PE", "PF", "PG", "PH", "PI").forEach { put(it, "NL") }
        listOf("ON", "OO", "OP", "OQ", "OR", "OS", "OT").forEach { put(it, "BE") }
        listOf("HB", "HE").forEach { put(it, "CH") }; put("HB0", "LI")
        listOf("OE").forEach { put(it, "AT") }
        listOf("OZ", "OU", "OV", "5P", "5Q").forEach { put(it, "DK") }
        listOf("SM", "SA", "SB", "SC", "SD", "SE", "SF", "SG", "SH", "SI", "SJ", "SK", "SL", "7S", "8S").forEach { put(it, "SE") }
        listOf("LA", "LB", "LC", "LD", "LE", "LF", "LG", "LH", "LI", "LJ", "LN").forEach { put(it, "NO") }
        listOf("OH", "OF", "OG", "OI", "OJ").forEach { put(it, "FI") }
        listOf("SP", "SN", "SO", "SQ", "SR", "3Z", "HF").forEach { put(it, "PL") }
        listOf("OK", "OL").forEach { put(it, "CZ") }
        listOf("OM").forEach { put(it, "SK") }
        listOf("HA", "HG").forEach { put(it, "HU") }
        listOf("YO", "YP", "YQ", "YR").forEach { put(it, "RO") }
        listOf("LZ").forEach { put(it, "BG") }
        listOf("SV", "SW", "SX", "SY", "SZ", "J4").forEach { put(it, "GR") }
        listOf("9A").forEach { put(it, "HR") }
        listOf("S5").forEach { put(it, "SI") }
        listOf("E7").forEach { put(it, "BA") }
        listOf("YU", "YT", "YZ").forEach { put(it, "RS") }
        listOf("Z3").forEach { put(it, "MK") }; put("Z6", "XK"); put("ZA", "AL")
        listOf("4O").forEach { put(it, "ME") }
        listOf("EI", "EJ").forEach { put(it, "IE") }
        listOf("LX").forEach { put(it, "LU") }
        listOf("LY").forEach { put(it, "LT") }
        listOf("YL").forEach { put(it, "LV") }
        listOf("ES").forEach { put(it, "EE") }
        listOf("EU", "EV", "EW").forEach { put(it, "BY") }
        listOf("UR", "US", "UT", "UU", "UV", "UW", "UX", "UY", "UZ", "EM", "EN", "EO").forEach { put(it, "UA") }
        listOf("ER").forEach { put(it, "MD") }
        listOf("R", "U").forEach { put(it, "RU") }
        listOf("UA", "UB", "UC", "UD", "UE", "UF", "UG", "UH", "UI", "RA", "RB", "RC", "RD", "RE", "RF", "RG", "RJ", "RK", "RL", "RM", "RN", "RO", "RQ", "RT", "RU", "RV", "RW", "RX", "RY", "RZ").forEach { put(it, "RU") }
        listOf("TA", "TB", "TC", "YM").forEach { put(it, "TR") }
        listOf("TF").forEach { put(it, "IS") }
        listOf("C3").forEach { put(it, "AD") }
        listOf("9H").forEach { put(it, "MT") }
        listOf("T7").forEach { put(it, "SM") }
        listOf("HV").forEach { put(it, "VA") }
        listOf("3A").forEach { put(it, "MC") }
        listOf("ZB", "ZG").forEach { put(it, "GI") }
        listOf("OY").forEach { put(it, "FO") }
        listOf("OX").forEach { put(it, "GL") }
        listOf("JW", "JX").forEach { put(it, "NO") }
        // Americas
        listOf("K", "N", "W", "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AI", "AJ", "AK", "AL").forEach { put(it, "US") }
        listOf("VE", "VA", "VO", "VY", "CY", "CF", "CG", "CH", "CI", "CJ", "CK", "VB", "VC", "VD", "VF", "VG", "VX").forEach { put(it, "CA") }
        listOf("XE", "XF", "4A", "4B", "4C", "6D", "6E", "6F", "6G", "6H", "6I", "6J").forEach { put(it, "MX") }
        listOf("PY", "PP", "PQ", "PR", "PS", "PT", "PU", "PV", "PW", "PX", "ZV", "ZW", "ZX", "ZY", "ZZ").forEach { put(it, "BR") }
        listOf("LU", "LO", "LP", "LQ", "LR", "LS", "LT", "LV", "LW", "AY", "AZ", "L2", "L3", "L4", "L5", "L6", "L7", "L8", "L9").forEach { put(it, "AR") }
        listOf("CE", "CA", "CB", "CC", "CD", "XQ", "XR", "3G").forEach { put(it, "CL") }
        listOf("CX", "CV", "CW").forEach { put(it, "UY") }
        listOf("ZP").forEach { put(it, "PY") }
        listOf("CP").forEach { put(it, "BO") }
        listOf("OA", "OB", "OC", "4T").forEach { put(it, "PE") }
        listOf("HC", "HD").forEach { put(it, "EC") }
        listOf("HK", "HJ", "5J", "5K").forEach { put(it, "CO") }
        listOf("YV", "YW", "YX", "YY", "4M").forEach { put(it, "VE") }
        listOf("HP", "HO", "H3", "H8", "H9", "3E", "3F").forEach { put(it, "PA") }
        listOf("TI", "TE").forEach { put(it, "CR") }
        listOf("TG", "TD").forEach { put(it, "GT") }
        listOf("YS", "HU").forEach { put(it, "SV") }
        listOf("HR", "HQ").forEach { put(it, "HN") }
        listOf("YN", "H6", "H7", "HT").forEach { put(it, "NI") }
        listOf("CO", "CM", "CL", "T4").forEach { put(it, "CU") }
        listOf("HI").forEach { put(it, "DO") }
        listOf("HH", "4V").forEach { put(it, "HT") }
        listOf("6Y").forEach { put(it, "JM") }
        listOf("KP4", "NP4", "WP4", "KP3", "NP3", "WP3").forEach { put(it, "PR") }
        listOf("ZF").forEach { put(it, "KY") }
        listOf("VP9").forEach { put(it, "BM") }
        listOf("8P").forEach { put(it, "BB") }
        listOf("9Y", "9Z").forEach { put(it, "TT") }
        listOf("P4").forEach { put(it, "AW") }
        listOf("PJ2", "PJ4").forEach { put(it, "CW") }
        listOf("FM").forEach { put(it, "MQ") }
        listOf("FG").forEach { put(it, "GP") }
        listOf("FY").forEach { put(it, "GF") }
        listOf("8R").forEach { put(it, "GY") }
        listOf("PZ").forEach { put(it, "SR") }
        listOf("V3").forEach { put(it, "BZ") }
        listOf("J3").forEach { put(it, "GD") }
        listOf("J6").forEach { put(it, "LC") }
        listOf("J7").forEach { put(it, "DM") }
        listOf("J8").forEach { put(it, "VC") }
        listOf("V4").forEach { put(it, "KN") }
        listOf("V2").forEach { put(it, "AG") }
        listOf("C6").forEach { put(it, "BS") }
        listOf("VP2E").forEach { put(it, "AI") }
        listOf("VP2M").forEach { put(it, "MS") }
        listOf("VP2V").forEach { put(it, "VG") }
        // Asia
        listOf("JA", "JE", "JF", "JG", "JH", "JI", "JJ", "JK", "JL", "JM", "JN", "JO", "JP", "JQ", "JR", "JS", "7J", "7K", "7L", "7M", "7N", "8J", "8N").forEach { put(it, "JP") }
        listOf("HL", "HM", "6K", "6L", "6M", "6N", "DS", "DT").forEach { put(it, "KR") }
        listOf("B", "BY", "BG", "BD", "BA", "BH", "BI", "BJ", "BL", "BT", "BZ", "3H", "3I", "3J", "3K", "3L", "3M", "3N", "3O", "3P", "3Q", "3R", "3S", "3T", "3U", "XS").forEach { put(it, "CN") }
        listOf("BV", "BX", "BM", "BN", "BO", "BP", "BQ", "BU", "BW").forEach { put(it, "TW") }
        listOf("VR2").forEach { put(it, "HK") }
        listOf("XX9").forEach { put(it, "MO") }
        listOf("VU", "AT", "AU", "AV", "AW", "8T", "8U", "8V", "8W", "8X", "8Y").forEach { put(it, "IN") }
        listOf("AP", "6P", "6Q", "6R", "6S").forEach { put(it, "PK") }
        listOf("S2", "S3").forEach { put(it, "BD") }
        listOf("4S").forEach { put(it, "LK") }
        listOf("9N").forEach { put(it, "NP") }
        listOf("A5").forEach { put(it, "BT") }
        listOf("XZ").forEach { put(it, "MM") }
        listOf("HS", "E2").forEach { put(it, "TH") }
        listOf("XV", "3W").forEach { put(it, "VN") }
        listOf("XU").forEach { put(it, "KH") }
        listOf("XW").forEach { put(it, "LA") }
        listOf("9M2", "9M4", "9W2", "9W4").forEach { put(it, "MY") }
        listOf("9M6", "9M8", "9W6", "9W8").forEach { put(it, "MY") }
        listOf("9V").forEach { put(it, "SG") }
        listOf("YB", "YC", "YD", "YE", "YF", "YG", "YH", "7A", "7B", "7C", "7D", "7E", "7F", "7G", "7H", "7I", "8A", "8B", "8C", "8D", "8E", "8F", "8G", "8H", "8I").forEach { put(it, "ID") }
        listOf("DU", "DV", "DW", "DX", "DY", "DZ", "4D", "4E", "4F", "4G", "4H", "4I").forEach { put(it, "PH") }
        listOf("V8").forEach { put(it, "BN") }
        listOf("UN", "UO", "UP", "UQ").forEach { put(it, "KZ") }
        listOf("UK", "UM").forEach { put(it, "UZ") }
        listOf("EX").forEach { put(it, "KG") }
        listOf("EY").forEach { put(it, "TJ") }
        listOf("EZ").forEach { put(it, "TM") }
        listOf("UJ", "UL").forEach { put(it, "UZ") }
        listOf("4J", "4K").forEach { put(it, "AZ") }
        listOf("4L").forEach { put(it, "GE") }
        listOf("EK").forEach { put(it, "AM") }
        listOf("EP", "EQ", "9B", "9C", "9D").forEach { put(it, "IR") }
        listOf("YI").forEach { put(it, "IQ") }
        listOf("YK", "6C").forEach { put(it, "SY") }
        listOf("OD").forEach { put(it, "LB") }
        listOf("4X", "4Z").forEach { put(it, "IL") }
        listOf("JY").forEach { put(it, "JO") }
        listOf("HZ", "7Z", "8Z").forEach { put(it, "SA") }
        listOf("A4").forEach { put(it, "OM") }
        listOf("A6").forEach { put(it, "AE") }
        listOf("A7").forEach { put(it, "QA") }
        listOf("A9").forEach { put(it, "BH") }
        listOf("9K").forEach { put(it, "KW") }
        listOf("7O").forEach { put(it, "YE") }
        listOf("YA", "T6").forEach { put(it, "AF") }
        listOf("JT", "JU", "JV").forEach { put(it, "MN") }
        listOf("P5").forEach { put(it, "KP") }
        // Africa
        listOf("ZS", "ZR", "ZT", "ZU").forEach { put(it, "ZA") }
        listOf("SU").forEach { put(it, "EG") }
        listOf("CN", "5C", "5D", "5E", "5F", "5G").forEach { put(it, "MA") }
        listOf("7X", "7Y").forEach { put(it, "DZ") }
        listOf("3V", "TS").forEach { put(it, "TN") }
        listOf("5A").forEach { put(it, "LY") }
        listOf("ST").forEach { put(it, "SD") }
        listOf("ET", "9E", "9F").forEach { put(it, "ET") }
        listOf("5Z", "5Y").forEach { put(it, "KE") }
        listOf("5H", "5I").forEach { put(it, "TZ") }
        listOf("5X").forEach { put(it, "UG") }
        listOf("9J").forEach { put(it, "ZM") }
        listOf("Z2").forEach { put(it, "ZW") }
        listOf("7Q").forEach { put(it, "MW") }
        listOf("C9").forEach { put(it, "MZ") }
        listOf("A2", "8O").forEach { put(it, "BW") }
        listOf("V5").forEach { put(it, "NA") }
        listOf("D2", "D3").forEach { put(it, "AO") }
        listOf("9Q", "9R", "9S", "9T").forEach { put(it, "CD") }
        listOf("TN").forEach { put(it, "CG") }
        listOf("TR").forEach { put(it, "GA") }
        listOf("TJ").forEach { put(it, "CM") }
        listOf("5N", "5O").forEach { put(it, "NG") }
        listOf("9G").forEach { put(it, "GH") }
        listOf("TU").forEach { put(it, "CI") }
        listOf("6W").forEach { put(it, "SN") }
        listOf("EL", "5L", "5M").forEach { put(it, "LR") }
        listOf("9L").forEach { put(it, "SL") }
        listOf("5T").forEach { put(it, "MR") }
        listOf("5U").forEach { put(it, "NE") }
        listOf("TZ").forEach { put(it, "ML") }
        listOf("XT").forEach { put(it, "BF") }
        listOf("TY").forEach { put(it, "BJ") }
        listOf("5V").forEach { put(it, "TG") }
        listOf("3X").forEach { put(it, "GN") }
        listOf("C5").forEach { put(it, "GM") }
        listOf("J5").forEach { put(it, "GW") }
        listOf("D4").forEach { put(it, "CV") }
        listOf("S9").forEach { put(it, "ST") }
        listOf("TL").forEach { put(it, "CF") }
        listOf("TT").forEach { put(it, "TD") }
        listOf("ST0").forEach { put(it, "SS") }
        listOf("E3").forEach { put(it, "ER") }
        listOf("J2").forEach { put(it, "DJ") }
        listOf("T5", "6O").forEach { put(it, "SO") }
        listOf("D6").forEach { put(it, "KM") }
        listOf("5R").forEach { put(it, "MG") }
        listOf("3B").forEach { put(it, "MU") }
        listOf("S7").forEach { put(it, "SC") }
        listOf("FR").forEach { put(it, "RE") }
        listOf("3DA").forEach { put(it, "SZ") }
        listOf("7P").forEach { put(it, "LS") }
        listOf("ZD7").forEach { put(it, "SH") }
        listOf("EA8", "EB8", "EC8").forEach { put(it, "IC") }
        listOf("CU").forEach { put(it, "PT") }
        listOf("IS0", "IM0").forEach { put(it, "IT") }
        // Oceania
        listOf("VK", "AX").forEach { put(it, "AU") }
        listOf("ZL", "ZM").forEach { put(it, "NZ") }
        listOf("YJ").forEach { put(it, "VU") }
        listOf("3D2").forEach { put(it, "FJ") }
        listOf("5W").forEach { put(it, "WS") }
        listOf("A3").forEach { put(it, "TO") }
        listOf("T2").forEach { put(it, "TV") }
        listOf("T30", "T31", "T32", "T33").forEach { put(it, "KI") }
        listOf("C2").forEach { put(it, "NR") }
        listOf("V6").forEach { put(it, "FM") }
        listOf("V7").forEach { put(it, "MH") }
        listOf("T8").forEach { put(it, "PW") }
        listOf("P2").forEach { put(it, "PG") }
        listOf("H4").forEach { put(it, "SB") }
        listOf("FK").forEach { put(it, "NC") }
        listOf("FO").forEach { put(it, "PF") }
        listOf("FW").forEach { put(it, "WF") }
        listOf("KH6", "AH6", "NH6", "WH6", "KH7", "AH7").forEach { put(it, "US") }
        listOf("KL", "AL7", "NL7", "WL7").forEach { put(it, "US") }
        listOf("KH2", "AH2", "NH2", "WH2").forEach { put(it, "GU") }
        listOf("9M").forEach { put(it, "MY") }
    }
}
