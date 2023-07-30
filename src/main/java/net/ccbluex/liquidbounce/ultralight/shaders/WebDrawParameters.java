/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
 */

package net.ccbluex.liquidbounce.ultralight.shaders;

import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL42.*;

public class WebDrawParameters {

    /**
     * This is a simple triangle which covers the whole screen.
     */
    private static final float[] VERTEX_DATA = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,

            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f
    };

    private final int shaderProgram;
    private final int vertexArray;
    private final int vertexBuffer;

    private final int textureUniform;

    public WebDrawParameters() {
        // Read the vertex and fragment shader sources
        String vertexShaderSource = readShaderSource("web.vert");
        String fragmentShaderSource = readShaderSource("web.frag");

        // Compile both shaders
        int vertexShader = compileShader(vertexShaderSource, GL_VERTEX_SHADER);
        int fragmentShader = compileShader(fragmentShaderSource, GL_FRAGMENT_SHADER);

        // Link the shaders into a program
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        // We can delete the shader objects after linking
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        // Make sure the program has linked successfully
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            String infoLog = glGetProgramInfoLog(program);
            throw new RuntimeException("Could not link program: " + infoLog);
        }

        this.shaderProgram = program;

        this.textureUniform = glGetUniformLocation(shaderProgram, "Texture");

        // We need a vertex array object
        this.vertexArray = glGenVertexArrays();

        // We also need a buffer which contains our vertex coordinates
        this.vertexBuffer = glGenBuffers();

        // Bind the buffer, the vertex data is always the same for our case
        glBindVertexArray(vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, VERTEX_DATA, GL_STATIC_DRAW);
    }

    public void activateShaderProgram() {
        // Set the vertex data as the first vertex attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Use the shader program
        glUseProgram(shaderProgram);
        glBindVertexArray(vertexArray);
    }

    public void setUniforms(int texture) {
        glUniform1i(textureUniform, texture);
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    private static int compileShader(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);

        if (status == GL_FALSE) {
            String infoLog = glGetShaderInfoLog(shader);
            throw new RuntimeException("Could not compile shader: " + infoLog);
        }

        return shader;
    }

    private static String readShaderSource(String name) {
        try (InputStream inputStream = WebDrawParameters.class.getResourceAsStream("/shaders/" + name)) {
            if (inputStream == null) {
                throw new RuntimeException("Could not find shader source " + name);
            }

            byte[] buffer = new byte[1024];
            StringBuilder stringBuilder = new StringBuilder();
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, read));
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not read shader source " + name, e);
        }
    }
}
