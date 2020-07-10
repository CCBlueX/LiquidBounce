#version 120

uniform float iTime;
uniform vec2 iResolution;

const float eps = 0.001;
const float daySpeed = 3.0;
const float season = -1.0;
const float followSun = 0.25;

vec3 rotationX(vec3 pos, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    vec3 rotPos;
    rotPos.x = pos.x;
    rotPos.y = c * pos.y - s * pos.z;
    rotPos.z = s * pos.y + c * pos.z;

    return rotPos;
}

/*
vec3 rotationY(vec3 pos, float angle) {
	float c = cos(angle);
    float s = sin(angle);
    vec3 rotPos;
	rotPos.x = c * pos.x + s * pos.z;
    rotPos.y = pos.y;
	rotPos.z = -s * pos.x + c * pos.z;

    return rotPos;
}

vec3 rotationZ(vec3 pos, float angle) {
	float c = cos(angle);
    float s = sin(angle);
    vec3 rotPos;
	rotPos.x = c * pos.x - s * pos.y;
    rotPos.y = c * pos.x + s * pos.y;
	rotPos.z = pos.z;

    return rotPos;
}
*/

vec2 rotuv(vec2 uv, float angle, vec2 center)
{
    return mat2(cos(angle), -sin(angle), sin(angle), cos(angle)) * (uv - center) + center;
}

float hash(float n)
{
    return fract(sin(dot(vec2(n,n) ,vec2(12.9898,78.233))) * 43758.5453);
}

float smin( float a, float b, float k )
{
    float h = clamp( 0.5+0.5*(b-a)/k, 0.0, 1.0 );
    return mix( b, a, h ) - k*h*(1.0-h);
}

float cell0(vec2 uv)
{
    vec2 uv2 = sin(uv);
    return uv2.x * uv2.y;
}

float cellCombine0(vec2 uv, float randSeed, float v, float s)
{
    float r1 = hash(randSeed);
    float angle1 = r1*360.0 + (r1*2.0-1.0)*iTime * 0.5*s;
    float c = cell0(rotuv(uv*(1.0 + r1*v), angle1, vec2(r1, hash(r1))));

    const int itterations = 3;

    for(int i = 0; i < itterations; i++)
    {
        float r = hash(float(i)*randSeed);
        float angle = r*360.0 + (r*2.0-1.0)*iTime * 0.5*s;
        float nc = cell0(rotuv(uv*(1.0 + r*v) + c, angle, vec2(r, hash(r))*10.0));
        c = mix(c, nc, 0.5);
    }

    return c;
}

float cell1(vec2 uv)
{
    vec2 uv2 = abs(sin(uv));
    return uv2.x * uv2.y;
}

float cellCombine1(vec2 uv, float randSeed, float v, float s)
{
    float r1 = hash(randSeed);
    float angle1 = r1*360.0 + (r1*2.0-1.0)*iTime * 0.5*s;
    float c = cell1(rotuv(uv*(1.0 + r1*v), angle1, vec2(r1, hash(r1))));

    const int itterations = 5;

    for(int i = 0; i < itterations; i++)
    {
        float r = hash(float(i)*randSeed);
        float angle = r*360.0 + (r*2.0-1.0)*iTime * 0.5*s;
        float nc = cell1(rotuv(uv*(1.0 + r*v) + c, angle, vec2(r, hash(r))*10.0));
        c = max(c, nc);
    }

    return c;
}

float cell2(vec2 uv)
{
    vec2 uv2 = abs(fract(uv) - vec2(0.5));
    return smoothstep(0.98, 1.0, (uv2.x+uv2.y));
}

float cellCombine2(vec2 uv, float randSeed, float v, float s)
{
    float r1 = hash(randSeed);
    float angle1 = r1*360.0 + (r1*2.0-1.0)*iTime * 0.5*s;
    float c = cell2(rotuv(uv*(1.0 + r1*v), angle1, vec2(r1, hash(r1))));

    const int itterations = 7;

    for(int i = 0; i < itterations; i++)
    {
        float r = hash(float(i)*randSeed);
        float angle = r*360.0 + (r*2.0-1.0)*iTime * 0.5*s;
        float nc = cell2(rotuv(uv*(1.0 + r*v), angle, vec2(r, hash(r))*10.0));
        c += nc*(0.5 + r);
    }

    return c;
}

float infinitePlane(vec3 pos, float height)
{
    return pos.y - height;
}

float distfunc(vec3 pos)
{
    float n0 = cellCombine0(pos.xz*0.3, 10.98765, 0.5, 0.0);
    float n1 = 1.0 - cellCombine1(pos.xz*0.2, 5.5678, 0.5, 0.0);
    float n2 = 1.0 - cellCombine1(pos.xz*vec2(1.5,3.0), 8.5548, 0.5, 0.0);
    return pos.y - n0*2.0 - n1*2.0 - n2*0.1;
}

vec2 rayMarch(vec3 rayDir, vec3 cameraOrigin)
{
    const int MAX_ITER = 100;
    const float MAX_DIST = 40.0;

    float totalDist = 0.0;
    vec3 pos = cameraOrigin;
    float dist = eps;

    for(int i = 0; i < MAX_ITER; i++)
    {
        dist = distfunc(pos);
        totalDist += dist;
        pos += dist*rayDir;

        if(dist < eps || totalDist > MAX_DIST)
        {
            break;
        }
    }
    return vec2(dist, totalDist);
}

vec3 skyBox(vec3 rayDir, vec3 sunDir, vec3 sunColor, float dayCycle1, float dayCycle2, float blur)
{
    vec3 rotDir = rotationX(rayDir, -iTime * 0.5 * 0.01);
    vec2 starUvs = vec2(atan(rotDir.x, rotDir.z), rotDir.y*4.0)*2.0;
    float stars = clamp(clamp(cellCombine2(starUvs, 3259.5741, 0.5, 0.0),0.0,1.0) + (1.0 - blur),0.0,1.0);
    vec3 skyColor1 = mix(vec3(0.05,0.05,0.2)+stars, mix(vec3(0.5,0.2,0.5),vec3(0.25,0.25,1.0), dayCycle1), dayCycle2);
    vec3 skyColor2 = mix(vec3(0.25,0.25,0.35), mix(vec3(0.8,0.4,0.2), vec3(1.0,1.0,1.0), dayCycle1), dayCycle2);
    vec3 groundColor1 = mix(vec3(0.25,0.22,0.3), mix(vec3(0.5,0.3,0.2), vec3(1.0,0.9,0.75), dayCycle1), dayCycle2);

    float sunPos = length(rayDir - sunDir);
    float sunBall = 1.0 - smoothstep(0.04, 0.05*blur, sunPos);
    float sunGlow = 1.0 - smoothstep(0.03, 0.5*blur, sunPos);
    float sunInnerGlow = 1.0 - smoothstep(0.0, 0.05*blur, sunPos);
    vec3 sun = ((sunBall + sunGlow)/blur)*sunColor + vec3(sunInnerGlow);

    vec3 skyColor = mix(skyColor2, skyColor1, clamp(rayDir.y*1.5,0.0,1.0));
    float m = smoothstep(0.0, 0.05, (rayDir.y+0.1)*blur);
    return mix(groundColor1, skyColor+sun, m);
}

vec3 lensFlare(vec3 rayDir, vec3 sunDir, vec3 sunColor, float dayCycle1)
{
    vec3 l = vec3(0.0);
    for(int i = 0; i < 4; i++)
    {
        float d = 0.22;
        float lensDistance = d + float(i)*d;
        vec3 rvec = vec3(0.0,0.0,1.0);
        vec3 sunvec = vec3(sunDir.x,-sunDir.y,sunDir.z);
        float lPos = length(rayDir - normalize(mix(sunDir, reflect(sunvec, rvec), lensDistance)));
        float growFactor = (1.0 + float(i)*float(i));
        float lGlow = 1.0 - smoothstep(0.01*growFactor, 0.05*growFactor, lPos);

        l += mix(vec3(1.0), vec3(0.5,0.5,2.0), lensDistance)*lensDistance*lGlow;
    }

    float lPosv = 1.0 - clamp(length(rayDir.xz - sunDir.xz),0.0,1.0);
    float lGlowv = pow(lPosv, 50.0);

    vec3 lens = (l + lGlowv)*sunColor*dayCycle1;

    return lens;
}

mat3 setCamera( in vec3 ro, in vec3 ta, float cr )
{
    vec3 cw = normalize(ta-ro);
    vec3 cp = vec3(sin(cr), cos(cr),0.0);
    vec3 cu = normalize( cross(cw,cp) );
    vec3 cv = normalize( cross(cu,cw) );
    return mat3( cu, cv, cw );
}

vec3 calculateNormals(vec3 pos)
{
    vec2 eps = vec2(0.0, 0.02);
    float X = distfunc(pos + eps.yxx) - distfunc(pos - eps.yxx);
    float Y = distfunc(pos + eps.xyx) - distfunc(pos - eps.xyx);
    float Z = distfunc(pos + eps.xxy) - distfunc(pos - eps.xxy);
    vec3 n = normalize(vec3(X,Y,Z));
    return n;
}

vec3 lighting(vec3 pos, vec3 rayDir, vec3 light, vec3 sunColor, float dayCycle1, float dayCycle2, vec3 n)
{
    vec3 ambSkyBox = skyBox(n, light, sunColor, dayCycle1, dayCycle2, 5.0);
    vec3 ambientColor = mix(ambSkyBox, clamp(ambSkyBox + 0.5, 0.0, 1.0), dayCycle1);
    vec3 diffuseColor = mix(vec3(1.0,0.7,0.5), vec3(1.0,0.9,0.6), pos.y*0.65);
    vec3 specColor = vec3(1.0,1.0,1.0);

    float diff = dot(normalize(light), n);
    float fresnel = (1.0 - 0.0, dot(n, -rayDir));
    vec3 r = reflect(normalize(rayDir), n);
    float spec = pow(max (0.0, dot (r, light)), 50.0);
    float specMap = cellCombine1(pos.xz*3.0, 12458.5125, 1.0, 0.0);

    vec3 res = diffuseColor*ambientColor;

    res += diffuseColor*max(0.0, diff)*sunColor;
    res += specColor*spec*sunColor*fresnel*smoothstep(0.95,1.0,specMap)*5.0;
    res += sunColor * max(0.0, -diff) * 0.35;

    return res;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec3 light = normalize(vec3(season,cos(daySpeed),sin(daySpeed)));

    vec2 screenPos = (fragCoord.xy/iResolution.xy)*2.0-1.0;
    screenPos.x *= iResolution.x/iResolution.y;

    vec3 cameraOrigin = vec3(iTime * 0.5, 4.0, iTime * 0.5);
    vec3 cameraTarget = cameraOrigin + mix(vec3(-1.0,0.0,0.0), light, followSun);

    mat3 cam = setCamera(cameraOrigin, cameraTarget, 0.0 );

    vec3 rayDir = cam* normalize( vec3(screenPos, 0.75) );
    vec2 dist = rayMarch(rayDir, cameraOrigin);

    float dayCycle = dot(light, vec3(0.0, 1.0, 0.0));
    float dayCycle1 = smoothstep(0.0, 0.5, max(0.0, dayCycle));
    float dayCycle2 = smoothstep(0.5, 1.0, min(dayCycle + 1.0, 1.0));
    vec3 sunColor = mix(vec3(0.5,0.2,0.0), vec3(0.4,0.4,0.2), dayCycle1);
    vec3 dayLight = mix(vec3(0.2, 0.2, 0.4), sunColor, dayCycle2);

    vec3 sky = skyBox(rayDir, light, dayLight, dayCycle1, dayCycle2, 1.0);
    vec3 lens = lensFlare(rayDir, light, dayLight, dayCycle1);
    vec3 res;

    if(dist.x < eps)
    {
        vec3 pos = cameraOrigin + dist.y*rayDir;
        vec3 n = calculateNormals(pos);
        float fog = clamp((dist.y - 18.0)*0.05, 0.0, 1.0);
        float fogY = (1.0 - clamp((pos.y+0.5)*0.5, 0.0, 1.0))*0.8;
        res = mix(lighting(pos, rayDir, light, dayLight, dayCycle1, dayCycle2, n), sky, clamp(fog+fogY, 0.0, 1.0));
    }
    else
    {
        res = sky;
    }

    float vign = 1.0 - smoothstep(0.5, 1.0, length(screenPos - vec2(0.0))*0.5);

    fragColor = vec4(res + vec3(lens), 1.0) * (0.75+vign*0.25);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}