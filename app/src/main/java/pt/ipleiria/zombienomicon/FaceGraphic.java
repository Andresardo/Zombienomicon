package pt.ipleiria.zombienomicon;
/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.zombienomicon.Model.GraphicOverlay;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final float EYE_RADIUS_PROPORTION = 0.45f;
    private static final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
    private final Paint mEyeWhitesPaint;
    private final Paint mEyeLidPaint;
    private final Paint mEyeIrisPaint;
    private final Paint mEyeOutlinePaint;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;
    // Record the previously seen proportions of the landmark locations relative to the bounding box
    // of the face.  These proportions can be used to approximate where the landmarks are within the
    // face bounding box if the eye landmark is missing in a future update.
    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();
    private volatile Face mFace;
    private int mFaceId;
    private boolean isZombie;
    private volatile PointF mLeftPosition;
    private volatile PointF mRightPosition;
    private boolean mLeftOpen;
    private boolean mRightOpen;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mFacePositionPaint = new Paint();

        mIdPaint = new Paint();
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        mEyeWhitesPaint = new Paint();
        mEyeWhitesPaint.setColor(Color.WHITE);
        mEyeWhitesPaint.setStyle(Paint.Style.FILL);

        mEyeLidPaint = new Paint();
        mEyeLidPaint.setColor(Color.YELLOW);
        mEyeLidPaint.setStyle(Paint.Style.FILL);

        mEyeIrisPaint = new Paint();
        mEyeIrisPaint.setColor(Color.BLACK);
        mEyeIrisPaint.setStyle(Paint.Style.FILL);

        mEyeOutlinePaint = new Paint();
        mEyeOutlinePaint.setColor(Color.BLACK);
        mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
        mEyeOutlinePaint.setStrokeWidth(5);
    }

    void setId(int id) {
        mFaceId = id;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        updatePreviousProportions(face);
        mLeftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        mRightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);
        mLeftOpen = mFace.getIsLeftEyeOpenProbability() > 0.5;
        mRightOpen = mFace.getIsRightEyeOpenProbability() > 0.5;

        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        if (isZombie) {
            mIdPaint.setColor(Color.RED);
            mBoxPaint.setColor(Color.RED);
            mFacePositionPaint.setColor(Color.RED);
        } else {
            mIdPaint.setColor(Color.GREEN);
            mBoxPaint.setColor(Color.GREEN);
            mFacePositionPaint.setColor(Color.GREEN);
        }
        canvas.drawText("H: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
        canvas.drawText("RE: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x - ID_X_OFFSET, y + ID_Y_OFFSET * 2, mIdPaint);
        canvas.drawText("LE: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET * 2, mIdPaint);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        if ((mLeftPosition != null) && (mRightPosition != null)) {
            PointF leftPosition = new PointF(translateX(mLeftPosition.x), translateY(mLeftPosition.y));
            PointF rightPosition = new PointF(translateX(mRightPosition.x), translateY(mRightPosition.y));

            // Use the inter-eye distance to set the size of the eyes.
            float distance = (float) Math.sqrt(Math.pow(rightPosition.x - leftPosition.x, 2) + Math.pow(rightPosition.y - leftPosition.y, 2));
            float eyeRadius = EYE_RADIUS_PROPORTION * distance;
            float irisRadius = IRIS_RADIUS_PROPORTION * distance;

            drawEye(canvas, leftPosition, eyeRadius, irisRadius, mLeftOpen);
            drawEye(canvas, rightPosition, eyeRadius, irisRadius, mRightOpen);
        }
    }

    void setZombie(boolean isZombie) {
        this.isZombie = isZombie;
    }

    private void updatePreviousProportions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            mPreviousProportions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }

    /**
     * Finds a specific landmark position, or approximates the position based on past observations
     * if it is not present.
     */
    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = mPreviousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }

    /**
     * Draws the eye, either closed or open with the iris in the middle.
     */
    private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius, float irisRadius, boolean isOpen) {
        if (isOpen) {
            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitesPaint);
            canvas.drawCircle(eyePosition.x, eyePosition.y, irisRadius, mEyeIrisPaint);
        } else {
            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeLidPaint);
            float y = eyePosition.y;
            float start = eyePosition.x - eyeRadius;
            float end = eyePosition.x + eyeRadius;
            canvas.drawLine(start, y, end, y, mEyeOutlinePaint);
        }
        canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeOutlinePaint);
    }
}