package net.ccbluex.liquidbounce.renderer.engine

import org.lwjgl.opengl.GL20

/**
 * Handles an instance of a shader (vertex, fragment, etc.). This is *not* a shader program.
 *
 * When this object is deallocated, it deletes the shader. Please never split the [id] from this object.
 *
 * @param source The source code of the shader. Not the resource location
 * @throws IllegalStateException When the shader fails to compile
 * @constructor Initializing this class is only possible from a thread that has an OpenGL context.
 */
private class Shader(shaderType: ShaderType, source: String) {
    /**
     * OpenGL's shader id
     */
    val id: Int

    /**
     * Was the shader deleted?
     */
    private var wasDeleted = false

    init {
        // Get an id from OpenGL
        this.id = GL20.glCreateShader(shaderType.typeConstant)

        // Give the source to the driver
        GL20.glShaderSource(this.id, source)
        // Make the driver compile the shader
        GL20.glCompileShader(this.id)

        // Check if the shader was compiled correctly
        if (GL20.glGetShaderi(this.id, GL20.GL_COMPILE_STATUS) != GL20.GL_TRUE)
            throw IllegalStateException("Shader failed to compile: ${GL20.glGetShaderInfoLog(this.id)}")
    }

    /**
     * Failsafe, if someone forgets to cleanup the shader
     */
    protected fun finalize() {
        delete()
    }

    /**
     * Deletes the shader, the id is not usable anymore
     */
    fun delete() {
        if (!wasDeleted) {
            GL20.glDeleteShader(this.id)

            wasDeleted = true
        }
    }

    enum class ShaderType(val typeConstant: Int) {
        VertexShader(GL20.GL_VERTEX_SHADER),
        FragmentShader(GL20.GL_FRAGMENT_SHADER),
    }
}

/**
 * A handler for OpenGL shader programs
 *
 * @constructor Creates a new shader program
 * @param vertexShaderSource The *source* of the fragment shader, not a resource location
 * @param fragmentShaderSource The *source* of the vertex shader, not a resource location
 * @throws IllegalStateException When one of the shaders failed to compile or the program failed to link
 */
class ShaderProgram(vertexShaderSource: String, fragmentShaderSource: String) {
    /**
     * OpenGL's program id
     */
    private val id: Int

    /**
     * Was the program deleted?
     */
    private var wasDeleted = false

    init {
        id = GL20.glCreateProgram()

        // Compile the two shaders
        val vertexShader = Shader(Shader.ShaderType.VertexShader, vertexShaderSource)
        val fragmentShader = Shader(Shader.ShaderType.FragmentShader, fragmentShaderSource)

        // Attach the shaders to the program
        GL20.glAttachShader(this.id, vertexShader.id)
        GL20.glAttachShader(this.id, fragmentShader.id)

        // Link the program
        GL20.glLinkProgram(this.id)

        // Check if the shader was compiled correctly
        if (GL20.glGetProgrami(this.id, GL20.GL_LINK_STATUS) != GL20.GL_TRUE)
            throw IllegalStateException("Program failed to link: ${GL20.glGetShaderInfoLog(this.id)}")

        // The shaders are linked into a program, we don't want to play with them anymore
        vertexShader.delete()
        fragmentShader.delete()


    }

    /**
     * Failsafe, if someone forgets to cleanup the shader
     */
    protected fun finalize() {
        delete()
    }

    /**
     * Deletes the program, the id is not usable anymore
     */
    fun delete() {
        if (!wasDeleted) {
            GL20.glDeleteShader(this.id)

            wasDeleted = true
        }
    }

}
