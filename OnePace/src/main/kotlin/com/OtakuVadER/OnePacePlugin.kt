package com.OtakuVadER

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class OnePacePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(OnepaceProvider())
        registerExtractorAPI(Streamruby())
        registerExtractorAPI(VidStream())
        registerExtractorAPI(Vidmolynet())
        registerExtractorAPI(GDMirrorbot())
        registerExtractorAPI(Cdnwish())
    }
}
