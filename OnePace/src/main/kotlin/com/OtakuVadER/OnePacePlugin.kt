package com.OtakuVadER

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class OnePacePlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(OnepaceProvider())
        registerExtractorAPI(Streamruby())
        registerExtractorAPI(VidStream())
        registerExtractorAPI(Vidmolynet())
        registerExtractorAPI(GDMirrorbot())
        registerExtractorAPI(Cdnwish())
    }
}
