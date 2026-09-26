#version 330
#extension GL_ARB_separate_shader_objects : require

// Ported from the GLSL shaders of Ultralight's AppCore (https://github.com/ultralight-ux/AppCore, tag v1.4.0b),
// which are licensed under the GNU Lesser General Public License 2.1.
//
// Colour textures hold BGRA pixels, which Ultralight produces, so they are swizzled when sampled and written.

layout(std140) uniform UltralightState {
    vec4 State;
    mat4 Transform;
    vec4 Scalar4[2];
    vec4 Vector[8];
    uint ClipSize;
    mat4 Clip[8];
};

float Scalar(uint i) { if (i < 4u) return Scalar4[0][i]; else return Scalar4[1][i - 4u]; }

uniform sampler2D Texture1;
uniform sampler2D Texture2;

layout(location = 0) in vec4 ex_Color;
layout(location = 1) in vec2 ex_TexCoord;
layout(location = 2) in vec2 ex_ObjectCoord;
layout(location = 3) in vec4 ex_Data0;
layout(location = 4) in vec4 ex_Data1;
layout(location = 5) in vec4 ex_Data2;
layout(location = 6) in vec4 ex_Data3;
layout(location = 7) in vec4 ex_Data4;
layout(location = 8) in vec4 ex_Data5;
layout(location = 9) in vec4 ex_Data6;

layout(location = 0) out vec4 fragColor;

vec4 out_Color = vec4(0.0);

uint FillType() { return uint(ex_Data0.x + 0.5); }
vec4 TileRectUV() { return Vector[0]; }
vec2 TileSize() { return Vector[1].zw; }
vec2 PatternTransformA() { return Vector[2].xy; }
vec2 PatternTransformB() { return Vector[2].zw; }
vec2 PatternTransformC() { return Vector[3].xy; }
uint Gradient_NumStops() { return uint(ex_Data0.y + 0.5); }
bool Gradient_IsRadial() { return bool(uint(ex_Data0.z + 0.5)); }
float Gradient_R0() { return ex_Data1.x; }
float Gradient_R1() { return ex_Data1.y; }
vec2 Gradient_P0() { return ex_Data1.xy; }
vec2 Gradient_P1() { return ex_Data1.zw; }
float SDFMaxDistance() { return ex_Data0.y; }

struct GradientStop { float percent; vec4 color; };

GradientStop GetGradientStop(uint offset) {
  GradientStop result;
  if (offset < 4u) {
    result.percent = ex_Data2[offset];
    if (offset == 0u)
      result.color = ex_Data3;
    else if (offset == 1u)
      result.color = ex_Data4;
    else if (offset == 2u)
      result.color = ex_Data5;
    else if (offset == 3u)
      result.color = ex_Data6;
  } else {
    result.percent = Scalar(offset - 4u);
    result.color = Vector[offset - 4u];
  }
  return result;
}

#define AA_WIDTH 0.354

float antialias(in float d, in float width, in float median) {
  return smoothstep(median - width, median + width, d);
}

float sdRect(vec2 p, vec2 size) {
    vec2 d = abs(p) - size;
    return min(max(d.x,d.y),0.0) + length(max(d,0.0));
}

// The below function "sdEllipse" is MIT licensed with following text:
//
// The MIT License
// Copyright 2013 Inigo Quilez
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software
// is furnished to do so, subject to the following conditions: The above copyright
// notice and this permission notice shall be included in all copies or substantial
// portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
// EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
// OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

float sdEllipse( vec2 p, in vec2 ab ) {
  if (abs(ab.x - ab.y) < 0.1)
    return length(p) - ab.x;

    p = abs(p); if (p.x > p.y) { p=p.yx; ab=ab.yx; }

    float l = ab.y*ab.y - ab.x*ab.x;

  float m = ab.x*p.x/l;
    float n = ab.y*p.y/l;
    float m2 = m*m;
    float n2 = n*n;

  float c = (m2 + n2 - 1.0)/3.0;
    float c3 = c*c*c;

  float q = c3 + m2*n2*2.0;
  float d = c3 + m2*n2;
  float g = m + m*n2;

  float co;

  if (d < 0.0)
  {
    float p = acos(q/c3)/3.0;
    float s = cos(p);
    float t = sin(p)*sqrt(3.0);
    float rx = sqrt( -c*(s + t + 2.0) + m2 );
    float ry = sqrt( -c*(s - t + 2.0) + m2 );
    co = ( ry + sign(l)*rx + abs(g)/(rx*ry) - m)/2.0;
  } else  {
    float h = 2.0*m*n*sqrt( d );
    float s = sign(q+h)*pow( abs(q+h), 1.0/3.0 );
    float u = sign(q-h)*pow( abs(q-h), 1.0/3.0 );
    float rx = -s - u - c*4.0 + 2.0*m2;
    float ry = (s - u)*sqrt(3.0);
    float rm = sqrt( rx*rx + ry*ry );
    float p = ry/sqrt(rm-rx);
    co = (p + 2.0*g/rm - m)/2.0;
  }

  float si = sqrt(1.0 - co*co);

  vec2 r = vec2(ab.x*co, ab.y*si);

  return length(r - p) * sign(p.y-r.y);
}

float sdRoundRect(vec2 p, vec2 size, vec4 rx, vec4 ry) {
  size *= 0.5;
  vec2 corner;

  corner = vec2(-size.x+rx.x, -size.y+ry.x);  // Top-Left
  vec2 local = p - corner;
  if (dot(rx.x, ry.x) > 0.0 && p.x < corner.x && p.y <= corner.y)
    return sdEllipse(local, vec2(rx.x, ry.x));

  corner = vec2(size.x-rx.y, -size.y+ry.y);   // Top-Right
  local = p - corner;
  if (dot(rx.y, ry.y) > 0.0 && p.x >= corner.x && p.y <= corner.y)
    return sdEllipse(local, vec2(rx.y, ry.y));

  corner = vec2(size.x-rx.z, size.y-ry.z);  // Bottom-Right
  local = p - corner;
  if (dot(rx.z, ry.z) > 0.0 && p.x >= corner.x && p.y >= corner.y)
    return sdEllipse(local, vec2(rx.z, ry.z));

  corner = vec2(-size.x+rx.w, size.y-ry.w); // Bottom-Left
  local = p - corner;
  if (dot(rx.w, ry.w) > 0.0 && p.x < corner.x && p.y > corner.y)
    return sdEllipse(local, vec2(rx.w, ry.w));

  return sdRect(p, size);
}

void fillSolid() {
  out_Color = ex_Color;
}

void fillImage(vec2 uv) {
  out_Color = texture(Texture1, uv).bgra * ex_Color;
}

vec2 transformAffine(vec2 val, vec2 a, vec2 b, vec2 c) {
  return val.x * a + val.y * b + c;
}

void fillPatternImage() {
  vec4 tile_rect_uv = TileRectUV();
  vec2 tile_size = TileSize();

  vec2 p = ex_ObjectCoord;

  // Apply the affine matrix
  vec2 transformed_coords = transformAffine(p,
    PatternTransformA(), PatternTransformB(), PatternTransformC());

  // Convert back to uv coordinate space
  transformed_coords /= tile_size;

  // Wrap UVs to [0.0, 1.0] so texture repeats properly
  vec2 uv = fract(transformed_coords);

  // Clip to tile-rect UV
  uv *= tile_rect_uv.zw - tile_rect_uv.xy;
  uv += tile_rect_uv.xy;

  fillImage(uv);
}

// Gradient noise from Jorge Jimenez's presentation:
// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
float gradientNoise(in vec2 uv)
{
    const vec3 magic = vec3(0.06711056, 0.00583715, 52.9829189);
    return fract(magic.z * fract(dot(uv, magic.xy)));
}

float ramp(in float inMin, in float inMax, in float val)
{
    return clamp((val - inMin) / (inMax - inMin), 0.0, 1.0);
}

void fillPatternGradient() {
  int num_stops = int(Gradient_NumStops());
  bool is_radial = Gradient_IsRadial();
  vec2 p0 = Gradient_P0();
  vec2 p1 = Gradient_P1();

  float t = 0.0;
  if (is_radial) {
    float r0 = p1.x;
    float r1 = p1.y;
    t = distance(ex_TexCoord, p0);
    float rDelta = r1 - r0;
    t = clamp((t / rDelta) - (r0 / rDelta), 0.0, 1.0);
  } else {
    vec2 V = p1 - p0;
    t = clamp(dot(ex_TexCoord - p0, V) / dot(V, V), 0.0, 1.0);
  }

  GradientStop stop0 = GetGradientStop(0u);
  GradientStop stop1 = GetGradientStop(1u);

  out_Color = mix(stop0.color, stop1.color, ramp(stop0.percent, stop1.percent, t));
  if (num_stops > 2) {
    GradientStop stop2 = GetGradientStop(2u);
    out_Color = mix(out_Color, stop2.color, ramp(stop1.percent, stop2.percent, t));
    if (num_stops > 3) {
      GradientStop stop3 = GetGradientStop(3u);
      out_Color = mix(out_Color, stop3.color, ramp(stop2.percent, stop3.percent, t));
      if (num_stops > 4) {
        GradientStop stop4 = GetGradientStop(4u);
        out_Color = mix(out_Color, stop4.color, ramp(stop3.percent, stop4.percent, t));
        if (num_stops > 5) {
          GradientStop stop5 = GetGradientStop(5u);
          out_Color = mix(out_Color, stop5.color, ramp(stop4.percent, stop5.percent, t));
          if (num_stops > 6) {
            GradientStop stop6 = GetGradientStop(6u);
            out_Color = mix(out_Color, stop6.color, ramp(stop5.percent, stop6.percent, t));
          }
        }
      }
    }
  }

  // Add gradient noise to reduce banding (+4/-4 gradations)
  //out_Color += (8.0/255.0) * gradientNoise(gl_FragCoord.xy) - (4.0/255.0);
}

void Unpack(vec4 x, out vec4 a, out vec4 b) {
  const float s = 65536.0;
  a = floor(x / s);
  b = floor(x - a * s);
}

const float epsilon = AA_WIDTH;

float antialias2 (float d) {
  return smoothstep (-epsilon, +epsilon, d);
}

// Returns two values:
// [0] = distance of p to line segment.
// [1] = closest t on line segment, clamped to [0, 1]
vec2 sdSegment(in vec2 p, in vec2 a, in vec2 b)
{
  vec2 pa = p - a, ba = b - a;
  float t = dot(pa, ba) / dot(ba, ba);
  return vec2(length(pa - ba * t), t);
}

float testCross(vec2 a, vec2 b, vec2 p) {
  return (b.y - a.y) * (p.x - a.x) - (b.x - a.x) * (p.y - a.y);
}

float sdLine(in vec2 a, in vec2 b, in vec2 p)
{
  vec2 pa = p - a, ba = b - a;
  float t = dot(pa, ba) / dot(ba, ba);
  return length(pa - ba*t) * sign(testCross(a, b, p));
}

vec4 blend(vec4 src, vec4 dest) {
  vec4 result;
  result.rgb = src.rgb + dest.rgb * (1.0 - src.a);
  result.a = src.a + dest.a * (1.0 - src.a);
  return result;
}



float innerStroke(float stroke_width, float d) {
  return min(antialias(-d, AA_WIDTH, 0.0), 1.0 - antialias(-d, AA_WIDTH, stroke_width));
}

void fillRoundedRect() {
  vec2 p = ex_TexCoord;
  vec2 size = ex_Data0.zw;
  p = (p - 0.5) * size;
  float d = sdRoundRect(p, size, ex_Data1, ex_Data2);

  // Fill background
  float alpha = antialias(-d, AA_WIDTH, 0.0);
  out_Color = ex_Color * alpha;

  // Draw stroke
  float stroke_width = ex_Data3.x;
  vec4 stroke_color = ex_Data4;

  if (stroke_width > 0.0) {
    alpha = innerStroke(stroke_width, d);
    vec4 stroke = stroke_color * alpha;
    out_Color = blend(stroke, out_Color);
  }
}

void fillBoxShadow() {
  vec2 p = ex_ObjectCoord;
  bool inset = bool(uint(ex_Data0.y + 0.5));
  float radius = ex_Data0.z;
  vec2 origin = ex_Data1.xy;
  vec2 size = ex_Data1.zw;
  vec2 clip_origin = ex_Data4.xy;
  vec2 clip_size = ex_Data4.zw;

  float d_clip = sdRoundRect(p - clip_origin, clip_size, ex_Data5, ex_Data6);
  float d_rect = sdRoundRect(p - origin, size, ex_Data2, ex_Data3);

  float clip = inset ? -d_rect : d_clip;
  float d = inset ? -d_clip : d_rect;

  if (clip < 0.0) {
    discard;
    out_Color = vec4(0.0, 0.0, 0.0, 0.0);
    return;
  }

  float alpha = radius >= 1.0? pow(antialias(-d, radius * 2.0 + 0.2, 0.0), 1.9) * 3.3 / pow(radius * 1.2, 0.15) :
                               antialias(-d, AA_WIDTH, inset ? -1.0 : 1.0);
  alpha = clamp(alpha, 0.0, 1.0) * ex_Color.a;
  out_Color = vec4(ex_Color.rgb * alpha, alpha);
  return;
}

vec3 blendOverlay(vec3 src, vec3 dest) {
  vec3 col;
  for (int i = 0; i < 3; ++i)
    col[i] = dest[i] < 0.5 ? (2.0 * dest[i] * src[i]) : (1.0 - 2.0 * (1.0 - dest[i]) * (1.0 - src[i]));
  return col;
}

vec3 blendColorDodge(vec3 src, vec3 dest) {
  vec3 col;
  for (int i = 0; i < 3; ++i)
    col[i] = (src[i] == 1.0) ? src[i] : min(dest[i] / (1.0 - src[i]), 1.0);
  return col;
}

vec3 blendColorBurn(vec3 src, vec3 dest) {
  vec3 col;
  for (int i = 0; i < 3; ++i)
    col[i] = (src[i] == 0.0) ? src[i] : max((1.0 - ((1.0 - dest[i]) / src[i])), 0.0);
  return col;
}

vec3 blendHardLight(vec3 src, vec3 dest) {
  vec3 col;
  for (int i = 0; i < 3; ++i)
    col[i] = dest[i] < 0.5 ? (2.0 * dest[i] * src[i]) : (1.0 - 2.0 * (1.0 - dest[i]) * (1.0 - src[i]));
  return col;
}

vec3 blendSoftLight(vec3 src, vec3 dest) {
  vec3 col;
  for (int i = 0; i < 3; ++i)
    col[i] = (src[i] < 0.5) ? (2.0 * dest[i] * src[i] + dest[i] * dest[i] * (1.0 - 2.0 * src[i])) : (sqrt(dest[i]) * (2.0 * src[i] - 1.0) + 2.0 * dest[i] * (1.0 - src[i]));
  return col;
}

vec3 rgb2hsl( vec3 col )
{
  const float eps = 0.0000001;
  float minc = min( col.r, min(col.g, col.b) );
  float maxc = max( col.r, max(col.g, col.b) );
  vec3 mask = step(col.grr,col.rgb) * step(col.bbg,col.rgb);
  vec3 h = mask * (vec3(0.0,2.0,4.0) + (col.gbr-col.brg)/(maxc-minc + eps)) / 6.0;
  return vec3(fract(1.0 + h.x + h.y + h.z ),                  // H
                (maxc-minc)/(1.0-abs(minc+maxc-1.0) + eps),   // S
                (minc+maxc)*0.5 );                            // L
}

vec3 hsl2rgb( vec3 c )
{
  vec3 rgb = clamp( abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0, 0.0, 1.0 );
  return c.z + c.y * (rgb-0.5)*(1.0-abs(2.0*c.z-1.0));
}

vec3 blendHue(vec3 src, vec3 dest) {
  vec3 baseHSL = rgb2hsl(dest);
  return hsl2rgb(vec3(rgb2hsl(src).r, baseHSL.g, baseHSL.b));
}

vec3 blendSaturation(vec3 src, vec3 dest) {
  vec3 baseHSL = rgb2hsl(dest);
  return hsl2rgb(vec3(baseHSL.r, rgb2hsl(src).g, baseHSL.b));
}

vec3 blendColor(vec3 src, vec3 dest) {
  vec3 blendHSL = rgb2hsl(src);
  return hsl2rgb(vec3(blendHSL.r, blendHSL.g, rgb2hsl(dest).b));
}

vec3 blendLuminosity(vec3 src, vec3 dest) {
  vec3 baseHSL = rgb2hsl(dest);
  return hsl2rgb(vec3(baseHSL.r, baseHSL.g, rgb2hsl(src).b));
}

vec4 saturate(vec4 val) {
  return clamp(val, 0.0, 1.0);
}

vec4 calcBlend() {
  const uint BlendOp_Clear = 0u;
  const uint BlendOp_Source = 1u;
  const uint BlendOp_Over = 2u;
  const uint BlendOp_In = 3u;
  const uint BlendOp_Out = 4u;
  const uint BlendOp_Atop = 5u;
  const uint BlendOp_DestOver = 6u;
  const uint BlendOp_DestIn = 7u;
  const uint BlendOp_DestOut = 8u;
  const uint BlendOp_DestAtop = 9u;
  const uint BlendOp_XOR = 10u;
  const uint BlendOp_Darken = 11u;
  const uint BlendOp_Add = 12u;
  const uint BlendOp_Difference = 13u;
  const uint BlendOp_Multiply = 14u;
  const uint BlendOp_Screen = 15u;
  const uint BlendOp_Overlay = 16u;
  const uint BlendOp_Lighten = 17u;
  const uint BlendOp_ColorDodge = 18u;
  const uint BlendOp_ColorBurn = 19u;
  const uint BlendOp_HardLight = 20u;
  const uint BlendOp_SoftLight = 21u;
  const uint BlendOp_Exclusion = 22u;
  const uint BlendOp_Hue = 23u;
  const uint BlendOp_Saturation = 24u;
  const uint BlendOp_Color = 25u;
  const uint BlendOp_Luminosity = 26u;

  fillImage(ex_TexCoord);
  vec4 src = out_Color;
  vec4 dest = texture(Texture2, ex_ObjectCoord).bgra;

  switch(uint(ex_Data0.y + 0.5))
  {
  case BlendOp_Clear: return vec4(0.0, 0.0, 0.0, 0.0);
  case BlendOp_Source: return src;
  case BlendOp_Over: return src + dest * (1.0 - src.a);
  case BlendOp_In: return src * dest.a;
  case BlendOp_Out: return src * (1.0 - dest.a);
  case BlendOp_Atop: return src * dest.a + dest * (1.0 - src.a);
  case BlendOp_DestOver: return src * (1.0 - dest.a) + dest;
  case BlendOp_DestIn: return dest * src.a;
  case BlendOp_DestOut: return dest * (1.0 - src.a);
  case BlendOp_DestAtop: return src * (1.0 - dest.a) + dest * src.a;
  case BlendOp_XOR: return saturate(src * (1.0 - dest.a) + dest * (1.0 - src.a));
  case BlendOp_Darken: return vec4(min(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_Add: return saturate(src + dest);
  case BlendOp_Difference: return vec4(abs(dest.rgb - src.rgb) * src.a, dest.a * src.a);
  case BlendOp_Multiply: return vec4(src.rgb * dest.rgb * src.a, dest.a * src.a);
  case BlendOp_Screen: return vec4((1.0 - ((1.0 - dest.rgb) * (1.0 - src.rgb))) * src.a, dest.a * src.a);
  case BlendOp_Overlay: return vec4(blendOverlay(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_Lighten: return vec4(max(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_ColorDodge: return vec4(blendColorDodge(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_ColorBurn: return vec4(blendColorBurn(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_HardLight: return vec4(blendOverlay(dest.rgb, src.rgb) * src.a, dest.a * src.a);
  case BlendOp_SoftLight: return vec4(blendSoftLight(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_Exclusion: return vec4((dest.rgb + src.rgb - 2.0 * dest.rgb * src.rgb) * src.a, dest.a * src.a);
  case BlendOp_Hue: return vec4(blendHue(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_Saturation: return vec4(blendSaturation(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_Color: return vec4(blendColor(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  case BlendOp_Luminosity: return vec4(blendLuminosity(src.rgb, dest.rgb) * src.a, dest.a * src.a);
  }

  return src;
}

void fillBlend() {
  out_Color = calcBlend();
}

void fillMask() {
  fillImage(ex_TexCoord);
  float alpha = texture(Texture2, ex_ObjectCoord).a;
  out_Color *= alpha;
}

void fillGlyph(vec2 uv) {
  float alpha = texture(Texture1, uv).r * ex_Color.a;
  alpha = clamp(alpha, 0.0, 1.0);
  float fill_color_luma = ex_Data0.y;
  float corrected_alpha = texture(Texture2, vec2(alpha, fill_color_luma)).r;
  //float corrected_alpha = alpha;
  out_Color = vec4(ex_Color.rgb * corrected_alpha, corrected_alpha);
}

void applyClip() {
  for (uint i = 0u; i < ClipSize; i++) {
    mat4 data = Clip[i];
    vec2 origin = data[0].xy;
    vec2 size = data[0].zw;
    vec4 radii_x, radii_y;
    Unpack(data[1], radii_x, radii_y);
    bool inverse = bool(data[3].z);

    vec2 p = ex_ObjectCoord;
    p = transformAffine(p, data[2].xy, data[2].zw, data[3].xy);
    p -= origin;

    float d_clip = sdRoundRect(p, size, radii_x, radii_y) * (inverse? -1.0 : 1.0);
    float alpha = antialias2(-d_clip);
    out_Color = vec4(out_Color.rgb * alpha, out_Color.a * alpha);

    //if (abs(d_clip) < 2.0)
    // out_Color = vec4(0.9, 1.0, 0.0, 1.0);
  }
}

void main(void) {
  const uint FillType_Solid = 0u;
  const uint FillType_Image = 1u;
  const uint FillType_Pattern_Image = 2u;
  const uint FillType_Pattern_Gradient = 3u;
  const uint FillType_RESERVED_1 = 4u;
  const uint FillType_RESERVED_2 = 5u;
  const uint FillType_RESERVED_3 = 6u;
  const uint FillType_Rounded_Rect = 7u;
  const uint FillType_Box_Shadow = 8u;
  const uint FillType_Blend = 9u;
  const uint FillType_Mask = 10u;
  const uint FillType_Glyph = 11u;

  switch (FillType())
  {
  case FillType_Solid: fillSolid(); break;
  case FillType_Image: fillImage(ex_TexCoord); break;
  case FillType_Pattern_Image: fillPatternImage(); break;
  case FillType_Pattern_Gradient: fillPatternGradient(); break;
  case FillType_Rounded_Rect: fillRoundedRect(); break;
  case FillType_Box_Shadow: fillBoxShadow(); break;
  case FillType_Blend: fillBlend(); break;
  case FillType_Mask: fillMask(); break;
  case FillType_Glyph: fillGlyph(ex_TexCoord); break;
  }

  applyClip();
  fragColor = out_Color.bgra;
}
