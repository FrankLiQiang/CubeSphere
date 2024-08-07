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
static int screenWidth, screenHeight, parentWidth;
static double factorA = 0, factorB = 0, factorC = 0, factorD = 0;
static float DepthZ, CenterX, CenterY, CenterZ, Center_x, Center_y, HPI, W2PI, W0, R2, R22, r, FirstR, parent_r;
static jbyte *BallBuf, *FatherBuf, *MotherBuf, *BallBufOut;

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
//    double Dot(const Vector3d &v) const {
//        return x * v.x + y * v.y + z * v.z;
//    }

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
    double c = ((EYE.x - BallCenter.x) * (EYE.x - BallCenter.x) + (EYE.y - BallCenter.y) * (EYE.y - BallCenter.y) +
                (EYE.z - BallCenter.z) * (EYE.z - BallCenter.z)) - Ra2;

    vector<double> t;
    SolvingQuadratics(a, b, c, t);

    for (auto it : t) {
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

void getPanel(Vector3d &p1, Vector3d &p2, Vector3d &p3, double &a, double &b, double &c, double &d) {
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

extern "C" JNIEXPORT void JNICALL Java_com_frank_cubesphere_MainActivity_initialization(JNIEnv *env, jobject obj,
                                                                                        const jint w, 
                                                                                        const jint h,
                                                                                        const jint ballRawWidth,
                                                                                        const jint ballRawHeight,
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
                for (auto it : points) {
                    if (it.z >= CenterZ) {
                        Vector3d P(it.x, it.y, it.z);
                        myMap[y][x] = P;
                    }
                }
            }
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_frank_cubesphere_MainActivity_ReadPics(JNIEnv *env, jobject obj,
                                                                                  const jbyteArray pBallData,
                                                                                  const jbyteArray pBallDataF,
                                                                                  const jbyteArray pBallDataM,
                                                                                  const jbyteArray pOutBall) {

    BallBuf = env->GetByteArrayElements(pBallData, 0);
    FatherBuf = env->GetByteArrayElements(pBallDataF, 0);
    MotherBuf = env->GetByteArrayElements(pBallDataM, 0);
    BallBufOut = env->GetByteArrayElements(pOutBall, 0);
}

extern "C" JNIEXPORT jint JNICALL Java_com_frank_cubesphere_MainActivity_transformsBall(JNIEnv *env, jobject obj,
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
            if (x < Center_x - r || x > Center_x + r || getDistance2(x, y, Center_x, Center_y) < r) {
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
