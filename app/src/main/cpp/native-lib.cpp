#include <jni.h>
#include <cstring>
#include <cmath>
#include <android/log.h>
#include <vector>
#include <string>

using namespace std;

//#define family
#define LOG_TAG "JNI.LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

const double EPSILON = 0.0000000001, PI = 3.1416f;
static int screenWidth, screenHeight, cubeWidth, parentWidth;
static double factorA = 0, factorB = 0, factorC = 0, factorD = 0;
static float DepthZ, CenterX, CenterY, CenterZ, Center_x, Center_y, HPI, W2PI, W0, R2, R22, r, FirstR, parent_r;
static jbyte *CubeBuf[6], *BallBuf, *FatherBuf, *MotherBuf, *CubeBufOut, *BallBufOut;

// 2D vector
struct Vector2d {
public:
    Vector2d() = default;

    ~Vector2d() = default;

    Vector2d(double dx, double dy) {
        x = dx;
        y = dy;
    }

    // 矢量赋值
//    void set(double dx, double dy) {
//        x = dx;
//        y = dy;
//    }

    // 矢量相加
    Vector2d operator+(const Vector2d &v) const {
        return {x + v.x, y + v.y};
    }

    // 矢量相减
    Vector2d operator-(const Vector2d &v) const {
        return {x - v.x, y - v.y};
    }

    //矢量数乘
//    Vector2d Scalar(double c) const {
//        return {c * x, c * y};
//    }

    bool operator==(const Vector2d &v) const {
        if (abs(x - v.x) < EPSILON && abs(y - v.y) < EPSILON) {
            return true;
        }
        return false;
    }

    double x{}, y{};
};

// 3D vector
struct Vector3d {
public:
    Vector3d() = default;

    ~Vector3d() = default;

    Vector3d(double dx, double dy, double dz) {
        x = dx;
        y = dy;
        z = dz;
    }

    // 矢量赋值
    void set(double dx, double dy, double dz) {
        x = dx;
        y = dy;
        z = dz;
    }

    // 矢量相加
    Vector3d operator+(const Vector3d &v) const {
        return {x + v.x, y + v.y, z + v.z};
    }

    // 矢量相减
    Vector3d operator-(const Vector3d &v) const {
        return {x - v.x, y - v.y, z - v.z};
    }

    //矢量数乘
    Vector3d Scalar(double c) const {
        return {c * x, c * y, c * z};
    }

    // 矢量点积
    double Dot(const Vector3d &v) const {
        return x * v.x + y * v.y + z * v.z;
    }

    // 矢量叉积
    Vector3d Cross(const Vector3d &v) const {
        return {y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x};
    }

    bool operator==(const Vector3d &v) const {
        if (abs(x - v.x) < EPSILON && abs(y - v.y) < EPSILON && abs(z - v.z) < EPSILON) {
            return true;
        }
        return false;
    }

    double x{}, y{}, z{};
};

static Vector3d BallCenter, EYE;
static Vector3d myMap[1280][1280];
static Vector2d FacePoints[4][4];

//求解一元二次方程组ax*x + b*x + c = 0
void SolvingQuadratics(double a, double b, double c, vector<double> &t) {
    double delta = b * b - 4 * a * c;

    if (delta < 0) {
        return;
    }

    if (abs(delta) < EPSILON) {
        t.push_back(-b / (2 * a));
    } else {
        t.push_back((-b + sqrt(delta)) / (2 * a));
        t.push_back((-b - sqrt(delta)) / (2 * a));
    }
}

void LineIntersectSphere(Vector3d &E, double Ra2, vector<Vector3d> &points) {
    Vector3d D = E - EYE;            //线段方向向量

    double a = (D.x * D.x) + (D.y * D.y) + (D.z * D.z);
    double b = (2 * D.x * (EYE.x - BallCenter.x) + 2 * D.y * (EYE.y - BallCenter.y) +
                2 * D.z * (EYE.z - BallCenter.z));
    double c = ((EYE.x - BallCenter.x) * (EYE.x - BallCenter.x) +
                (EYE.y - BallCenter.y) * (EYE.y - BallCenter.y) +
                (EYE.z - BallCenter.z) * (EYE.z - BallCenter.z)) - Ra2;

    vector<double> t;
    SolvingQuadratics(a, b, c, t);

    for (auto it: t) {
        points.push_back(EYE + D.Scalar(it));
    }
}

double getDistance2(float x1, float y1, float x2, float y2) {
    return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

double getDistance32(Vector3d &P1, Vector3d &P2) {
    double ret = (P1.x - P2.x) * (P1.x - P2.x) + (P1.y - P2.y) * (P1.y - P2.y) +
                 +(P1.z - P2.z) * (P1.z - P2.z);
    return ret;
}

Vector3d GetFootOfPerpendicular(const Vector3d &p0, const Vector3d &p1, const Vector3d &p2) {
    double dx = p2.x - p1.x;
    double dy = p2.y - p1.y;
    double dz = p2.z - p1.z;

    double k = ((p1.x - p0.x) * dx + (p1.y - p0.y) * dy + (p1.z - p0.z) * dz)
               / ((dx * dx) + (dy * dy) + (dz * dz)) * -1;

    Vector3d retVal(k * dx + p1.x, k * dy + p1.y, k * dz + p1.z);

    return retVal;
}

void
getPanel(Vector3d &p1, Vector3d &p2, Vector3d &p3, double &a, double &b, double &c, double &d) {
    a = ((p2.y - p1.y) * (p3.z - p1.z) - (p2.z - p1.z) * (p3.y - p1.y));
    b = ((p2.z - p1.z) * (p3.x - p1.x) - (p2.x - p1.x) * (p3.z - p1.z));
    c = ((p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x));
    d = (0 - (a * p1.x + b * p1.y + c * p1.z));
}

void getBallShowCenter() {
    Center_x = screenWidth / 2;
    Center_y = screenHeight / 2;        //if the 2D Ball center and screen center is same point.
    //Center_y = (EYE.z - DepthZ) * ((BallCenter.y - EYE.y) / (EYE.z - BallCenter.z));
}

bool isBack(Vector3d &p) {
    return factorA * p.x + factorB * p.y + factorC * p.z + factorD > 0;
}

double getLongitude(Vector3d &P, Vector3d &A, Vector3d &M) {
    Vector3d foot = GetFootOfPerpendicular(P, A, BallCenter);   //点P 到 北极，球心连线的 垂足
    Vector3d a = P - foot;                                      // 垂足到点P的方向向量
    Vector3d b = M - BallCenter;                                // 本初子午线与赤道的交点与地心的方向向量

    double c = (a.x * b.x + a.y * b.y + a.z * b.z) / sqrt(a.x * a.x + a.y * a.y + a.z * a.z)
               / sqrt(b.x * b.x + b.y * b.y + b.z * b.z);

    if (isBack(P)) {
        return 2 * PI - acos(c);
    }
    return acos(c);
}

float cross(Vector2d a, Vector2d b) { return a.x * b.y - a.y * b.x; }

Vector2d invBilinear(Vector2d p, int index) {

    Vector2d a = FacePoints[index][1];
    Vector2d b = FacePoints[index][0];
    Vector2d c = FacePoints[index][3];
    Vector2d d = FacePoints[index][2];

    Vector2d e = b - a;
    Vector2d f = d - a;
    Vector2d g = a - b + c - d;
    Vector2d h = p - a;

    float k2 = cross(g, f);
    float k1 = cross(e, f) + cross(h, g);
    float k0 = cross(h, e);

    if (abs(k2) < 0.001) {
        return Vector2d((h.x * k1 + f.x * k0) / (e.x * k1 - g.x * k0), -k0 / k1);
    }

    float w = k1 * k1 - 4.0 * k0 * k2;
    if (w < 0.0) return Vector2d(-1, 0);

    w = sqrt(w);

    float v = (-k1 - w) / (2.0 * k2);
    float u = (h.x - f.x * v) / (e.x + g.x * v);

    if (v < 0.0 || v > 1.0 || u < 0.0 || u > 1.0) {
        v = (-k1 + w) / (2.0 * k2);
        u = (h.x - f.x * v) / (e.x + g.x * v);
    }
    if (v < 0.0 || v > 1.0 || u < 0.0 || u > 1.0) {
        v = -1.0;
        u = -1.0;
    }

    return Vector2d(u, v);
}

struct quat {
    Vector2d points[4];
};

bool inquat(int index, float x, float y) {
    Vector2d v1, v2;
    for (int i = 0; i < 4; i++) {
        int i1 = (i + 1) % 4;
        v1.x = FacePoints[index][i1].x - FacePoints[index][i].x;
        v1.y = FacePoints[index][i1].y - FacePoints[index][i].y;
        v2.x = x - FacePoints[index][i].x;
        v2.y = y - FacePoints[index][i].y;
        if (cross(v2, v1) < 0) {
            return false;
        }
    }
    return true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_frank_cubesphere_MainActivity_initialization(JNIEnv *env, jobject obj,
                                                      const jint w,
                                                      const jint h,
                                                      const jint ballRawWidth,
                                                      const jint ballRawHeight,
                                                      const jint cubeW,
                                                      const jint widthP,
                                                      const jfloat e_x,
                                                      const jfloat e_y,
                                                      const jfloat e_z,
                                                      const jfloat Z,
                                                      const jfloat cx,
                                                      const jfloat cy,
                                                      const jfloat cz,
                                                      const jfloat R0,
                                                      const jfloat r0) {
    screenWidth = w;
    screenHeight = h;
    EYE.set(e_x, e_y, e_z);
    DepthZ = Z;
    CenterX = cx;
    CenterY = cy;
    CenterZ = cz;
    BallCenter.set(CenterX, CenterY, CenterZ);
    cubeWidth = cubeW;
    HPI = ballRawHeight / PI;
    W0 = ballRawWidth;
    W2PI = ballRawWidth / PI / 2.0f;
    r = r0, FirstR = r;
    R22 = R0 * R0;
    R2 = 2 * R22;
    getBallShowCenter();
    parentWidth = widthP;
    parent_r = widthP / 2;

    for (int y = 0; y < screenHeight; y++) {
        if (y < Center_y - r || y > Center_y + r) {
            continue;
        }
        for (int x = 0; x < screenWidth; x++) {
            if (getDistance2(x, y, Center_x, Center_y) < r) {
                Vector3d E(x, y, DepthZ);
                vector<Vector3d> points;
                LineIntersectSphere(E, R22, points);
                for (auto it: points) {
                    if (it.z >= CenterZ) {
                        Vector3d P(it.x, it.y, it.z);
                        myMap[y][x] = P;
                    }
                }
            }
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_frank_cubesphere_MainActivity_ReadPics(JNIEnv *env, jobject obj,
                                                const jbyteArray pSrcData1,
                                                const jbyteArray pSrcData2,
                                                const jbyteArray pSrcData3,
                                                const jbyteArray pSrcData4,
                                                const jbyteArray pSrcData5,
                                                const jbyteArray pSrcData6,
                                                const jbyteArray pBallData,
                                                const jbyteArray pBallDataF,
                                                const jbyteArray pBallDataM,
                                                const jbyteArray pOutBall,
                                                const jbyteArray pOutCube) {

    CubeBuf[0] = env->GetByteArrayElements(pSrcData1, 0);
    CubeBuf[1] = env->GetByteArrayElements(pSrcData2, 0);
    CubeBuf[2] = env->GetByteArrayElements(pSrcData3, 0);
    CubeBuf[3] = env->GetByteArrayElements(pSrcData4, 0);
    CubeBuf[4] = env->GetByteArrayElements(pSrcData5, 0);
    CubeBuf[5] = env->GetByteArrayElements(pSrcData6, 0);

    BallBuf = env->GetByteArrayElements(pBallData, 0);
    FatherBuf = env->GetByteArrayElements(pBallDataF, 0);
    MotherBuf = env->GetByteArrayElements(pBallDataM, 0);

    CubeBufOut = env->GetByteArrayElements(pOutCube, 0);
    BallBufOut = env->GetByteArrayElements(pOutBall, 0);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_frank_cubesphere_MainActivity_transformsBall(JNIEnv *env, jobject obj,
                                                      const jfloat ArcticX,
                                                      const jfloat ArcticY,
                                                      const jfloat ArcticZ,
                                                      const jfloat MeridianX,
                                                      const jfloat MeridianY,
                                                      const jfloat MeridianZ,
                                                      const jfloat r0) {
    r = r0;
    double scale = r / FirstR;
    Vector3d Arctic(ArcticX, ArcticY, ArcticZ);
    Vector3d Meridian(MeridianX, MeridianY, MeridianZ);

    getPanel(Arctic, Meridian, BallCenter, factorA, factorB, factorC, factorD);

    double latitude, longitude;
    int original_point, pixel_point, x0, y0;

#ifdef family
    float alpha1 = 0.523598f;       //PI / 6.0f;
    float alpha2 = 2.61799f;        //PI * 5 / 6.0f;
#endif
    for (int y = 0; y < screenHeight; y++) {
        if (y < Center_y - r || y > Center_y + r) {
            for (int x = 0; x < screenWidth; x++) {
                pixel_point = (screenWidth * y + x) * 4;
                *(BallBufOut + pixel_point) = 0;
                *(BallBufOut + pixel_point + 1) = 0;
                *(BallBufOut + pixel_point + 2) = 0;
                *(BallBufOut + pixel_point + 3) = 0;
            }
            continue;
        }
        for (int x = 0; x < screenWidth; x++) {
            pixel_point = (screenWidth * y + x) * 4;
            if (x < Center_x - r || x > Center_x + r ||
                getDistance2(x, y, Center_x, Center_y) >= r) {
                *(BallBufOut + pixel_point) = 0;
                *(BallBufOut + pixel_point + 1) = 0;
                *(BallBufOut + pixel_point + 2) = 0;
                *(BallBufOut + pixel_point + 3) = 0;
                continue;
            }
            Vector3d P = myMap[y][x];
            if (r == FirstR) {
                P = myMap[y][x];
            } else {
                x0 = (int) ((double) screenWidth / 2.0f +
                            ((double) x - (double) screenWidth / 2.0f) / scale);
                y0 = (int) ((double) screenHeight / 2.0f +
                            ((double) y - (double) screenHeight / 2.0f) / scale);
                if (x0 < 0) x0 = 0;
                if (x0 >= screenWidth) x0 = screenWidth - 1;
                if (y0 < 0) y0 = 0;
                if (y0 >= screenHeight) y0 = screenHeight - 1;
                P = myMap[y0][x0];
            }
            latitude = acos(1 - getDistance32(P, Arctic) / R2);
            longitude = getLongitude(P, Arctic, Meridian);
#ifdef family
            if (latitude < alpha1) {
                int x_index = (round)(parent_r - cos(longitude + PI) * parent_r * (latitude / alpha1));
                int y_index = (round)(parent_r - sin(longitude + PI) * parent_r * (latitude / alpha1));
                original_point = (y_index * parentWidth + x_index) * 4;
                if (original_point >= 0 && original_point < parentWidth * parentWidth * 4) {
                    *(BallBufOut + pixel_point) = *(FatherBuf + original_point);
                    *(BallBufOut + pixel_point + 1) = *(FatherBuf + original_point + 1);
                    *(BallBufOut + pixel_point + 2) = *(FatherBuf + original_point + 2);
                    *(BallBufOut + pixel_point + 3) = -1;
                }
            } else if (latitude > alpha2) {
                int x_index = (round)(parent_r - cos(longitude) * parent_r * ((PI - latitude) / (PI - alpha2)));
                int y_index = (round)(parent_r - sin(longitude) * parent_r * ((PI - latitude) / (PI - alpha2)));
                original_point = (y_index * parentWidth + x_index) * 4;
                if (original_point >= 0 && original_point < parentWidth * parentWidth * 4) {
                    *(BallBufOut + pixel_point) = *(MotherBuf + original_point);
                    *(BallBufOut + pixel_point + 1) = *(MotherBuf + original_point + 1);
                    *(BallBufOut + pixel_point + 2) = *(MotherBuf + original_point + 2);
                    *(BallBufOut + pixel_point + 3) = -1;
                }
            } else {
#endif
            original_point = ((int) (HPI * latitude) * W0 + (int) (W2PI * longitude)) * 4;
            if (original_point >= 0 && original_point < W0 * W0 * 8) {
                *(BallBufOut + pixel_point) = *(BallBuf + original_point);
                *(BallBufOut + pixel_point + 1) = *(BallBuf + original_point + 1);
                *(BallBufOut + pixel_point + 2) = *(BallBuf + original_point + 2);
                *(BallBufOut + pixel_point + 3) = -1;
            }
#ifdef family
            }
#endif
        }
    }
    return 0;
}

extern "C" JNIEXPORT jint JNICALL Java_com_frank_cubesphere_MainActivity_isCubeVisible(JNIEnv *env, jobject obj,
                                                                                       const jfloat xE,
                                                                                       const jfloat yE,
                                                                                       const jfloat zE,
                                                                                       const jfloat xF,
                                                                                       const jfloat yF,
                                                                                       const jfloat zF,
                                                                                       const jfloat xG,
                                                                                       const jfloat yG,
                                                                                       const jfloat zG) {
    Vector3d E(xE, yE, zE);
    Vector3d F(xF, yF, zF);
    Vector3d G(xG, yG, zG);

    Vector3d EF = F - E;
    Vector3d EG = G - E;
    Vector3d N = EF.Cross(EG);
    Vector3d V = EYE - E;

    return N.Dot(V) <= 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_frank_cubesphere_MainActivity_transformsCube(JNIEnv *env, jobject obj,
                                                      jint count,
                                                      const jintArray index,
                                                      const jobjectArray facePointsX,
                                                      const jobjectArray facePointsY) {

    int original_point, pixel_point, X, Y;
    jint *picIndex = env->GetIntArrayElements((jintArray) index, 0);

    for (int i = 0; i < count; i++) {
        jobject myarrayx = env->GetObjectArrayElement(facePointsX, i);
        jobject myarrayy = env->GetObjectArrayElement(facePointsY, i);

        jfloat *datax = env->GetFloatArrayElements((jfloatArray) myarrayx, 0);
        jfloat *datay = env->GetFloatArrayElements((jfloatArray) myarrayy, 0);
        for (int j = 0; j < 4; j++) {
            FacePoints[i][j].x = datax[j];
            FacePoints[i][j].y = datay[j];
        }
    }

    bool isInside;
    for (int y = 0; y < screenHeight; y++) {
        for (int x = 0; x < screenWidth; x++) {
            pixel_point = (screenWidth * y + x) * 4;
            Vector2d p(x, y);
            isInside = false;
            for (int i = 0; i < count; i++) {
                if (inquat(i, x, y)) {
                    isInside = true;
                    Vector2d v2 = invBilinear(p, i);
                    X = (int) ((float) cubeWidth * v2.x);
                    Y = (int) ((float) cubeWidth * v2.y);
                    original_point = (Y * cubeWidth + X) * 4;
                    if (original_point >= 0 && original_point < cubeWidth * cubeWidth * 4) {
                        *(CubeBufOut + pixel_point) = *(CubeBuf[picIndex[i]] + original_point);
                        *(CubeBufOut + pixel_point + 1) = *(CubeBuf[picIndex[i]] + original_point + 1);
                        *(CubeBufOut + pixel_point + 2) = *(CubeBuf[picIndex[i]] + original_point + 2);
                        *(CubeBufOut + pixel_point + 3) = -1;
                    }
                    break;
                }
            }
            if (!isInside) {
                *(CubeBufOut + pixel_point) = 0;
                *(CubeBufOut + pixel_point + 1) = 0;
                *(CubeBufOut + pixel_point + 2) = 0;
                *(CubeBufOut + pixel_point + 3) = 0;
            }
        }
    }
}
