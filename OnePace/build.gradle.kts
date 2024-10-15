version = 2


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "One Pace"
    authors = listOf("KillerDogeEmpire, HindiProvider")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime"
    )

    iconUrl = "https://raw.githubusercontent.com/OtakuVadER/OnePace-Extension/logos/onepace.png"
}
