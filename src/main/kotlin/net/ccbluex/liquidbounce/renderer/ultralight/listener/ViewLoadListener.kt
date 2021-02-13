/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 * and
 *
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.ccbluex.liquidbounce.renderer.ultralight.listener

import com.labymedia.ultralight.Databind
import com.labymedia.ultralight.DatabindConfiguration
import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.api.JavaAPI
import com.labymedia.ultralight.plugin.loading.UltralightLoadListener
import net.ccbluex.liquidbounce.renderer.ultralight.support.ViewContextProvider

class ViewLoadListener(private val view: UltralightView) : UltralightLoadListener {

    private val databind = Databind(
        DatabindConfiguration
            .builder()
            .contextProviderFactory(ViewContextProvider.Factory(view))
            .build()
    )
    private val javaApi = JavaAPI(databind)

    /**
     * Helper function to construct a name for a frame from a given set of parameters.
     *
     * @param frameId     The id of the frame
     * @param isMainFrame Whether the frame is the main frame on the page
     * @param url         The URL of the frame
     * @return A formatted frame name
     */
    private fun frameName(frameId: Long, isMainFrame: Boolean, url: String): String {
        return "[${if (isMainFrame) "MainFrame" else "Frame"} $frameId ($url)]: "
    }

    /**
     * Called by Ultralight when a frame in a view beings loading.
     *
     * @param frameId     The id of the frame that has begun loading
     * @param isMainFrame Whether the frame is the main frame
     * @param url         The url that the frame started to load
     */
    override fun onBeginLoading(frameId: Long, isMainFrame: Boolean, url: String) {
        println("${frameName(frameId, isMainFrame, url)}The view is about to load")
    }

    /**
     * Called by Ultralight when a frame in a view finishes loading.
     *
     * @param frameId     The id of the frame that finished loading
     * @param isMainFrame Whether the frame is the main frame
     * @param url         The url the frame has loaded
     */
    override fun onFinishLoading(frameId: Long, isMainFrame: Boolean, url: String) {
        println("${frameName(frameId, isMainFrame, url)}The view finished loading")
    }

    /**
     * Called by Ultralight when a frame in a view fails to load.
     *
     * @param frameId     The id of the frame that failed to load
     * @param isMainFrame Whether the frame is the main frame
     * @param url         The url that failed to load
     * @param description A description of the error
     * @param errorDomain The domain that failed to load
     * @param errorCode   An error code indicating the error reason
     */
    override fun onFailLoading(frameId: Long, isMainFrame: Boolean, url: String, description: String,
                               errorDomain: String, errorCode: Int) {
        System.err.println("${frameName(frameId, isMainFrame, url)}Failed to load $errorDomain, $errorCode($description)")
    }

    /**
     * Called by Ultralight when the history of a view changes.
     */
    override fun onUpdateHistory() {
        println("The view has updated the history")
    }

    /**
     * Called by Ultralight when the window object is ready. This point can be used to inject Javascript.
     *
     * @param frameId     The id of the frame that the object became ready in
     * @param isMainFrame Whether the frame is the main frame
     * @param url         The url that the frame currently contains
     */
    override fun onWindowObjectReady(frameId: Long, isMainFrame: Boolean, url: String) {
        view.lockJavascriptContext().use { lock ->
            // Retrieve the context and convert it to a global context
            val context = lock.context
            val globalContext = context.globalContext

            // Retrieve the global object (the window object)
            val globalObject = globalContext.globalObject

            // Set the JavaAPI as a java on it
            // Javascript will now be able to access Java classes using "java.importClass('class name')"
            //
            // Of course you could set any other arbitrary Java object here and Javascript would be able to access it.
            //
            // You can also set Javascript values.
            val translatedApi = databind.conversionUtils.toJavascript(context, javaApi)
            globalObject.setProperty("java", translatedApi, 0)
        }
    }

    /**
     * Called by Ultralight when the DOM is ready. This point can be used to inject Javascript.
     *
     * @param frameId     The id of the frame that the DOM became ready in
     * @param isMainFrame Whether the frame is the main frame
     * @param url         The url that the frame currently contains
     */
    override fun onDOMReady(frameId: Long, isMainFrame: Boolean, url: String) { }

}
