/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Picture;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.commonsware.cwac.cam2.util.Utils;
import com.github.polok.flipview.FlipView;
import java.io.File;
import de.greenrobot.event.EventBus;

/**
 * Fragment for displaying a camera preview, with hooks to allow
 * you (or the user) to take a picture.
 */
public class CameraFragment extends Fragment {
  /**
   * Interface that all hosting activities must implement.
   */
  public interface Contract {
    /**
     * Used by CameraFragment to indicate that the user has
     * taken a photo, for activities that wish to take a specific
     * action at this point (e.g., set a result and finish).
     */
    void completeRequest();
  }

  private CameraController ctrl;

  /**
   * Standard fragment entry point.
   *
   * @param savedInstanceState State of a previous instance
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
  }

  /**
   * Standard lifecycle method, for when the fragment becomes
   * attached to an activity. Used here to validate the contract.
   *
   * @param activity the hosting activity
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!(activity instanceof Contract)) {
      throw new IllegalArgumentException("Hosting activity must implement CameraFragment.Contract");
    }
  }

  /**
   * Standard lifecycle method, for when the fragment moves into
   * the started state. Passed along to the CameraController.
   */
  @Override
  public void onStart() {
    super.onStart();

    EventBus.getDefault().register(this);

    if (ctrl!=null) {
      ctrl.start();
    }
  }

  /**
   * Standard lifecycle method, for when the fragment moves into
   * the stopped state. Passed along to the CameraController.
   */
  @Override
  public void onStop() {
    if (ctrl!=null) {
      ctrl.stop();
    }

    EventBus.getDefault().unregister(this);

    super.onStop();
  }

  /**
   * Standard lifecycle method, for when the fragment is utterly,
   * ruthlessly destroyed. Passed along to the CameraController,
   * because why should the fragment have all the fun?
   */
  @Override
  public void onDestroy() {
    if (ctrl!=null) {
      ctrl.destroy();
    }

    super.onDestroy();
  }

  /**
   * Standard callback method to create the UI managed by
   * this fragment.
   *
   * @param inflater Used to inflate layouts
   * @param container Parent of the fragment's UI (eventually)
   * @param savedInstanceState State of a previous instance
   * @return the UI being managed by this fragment
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    CameraView cv=new CameraView(getActivity());

    cv.setEngine(ctrl.getEngine());
    ctrl.setCameraView(cv);

    cv.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        File dcim=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File dir=new File(dcim, "Cam2");

        dir.mkdirs();

        File testOutput=new File(dir, "test.jpg");

        PictureTransaction xact=new PictureTransaction.Builder()
            .toFile(testOutput.getAbsolutePath(), true).build();

        ctrl.takePicture(xact);

        // getContract().completeRequest();

        return(true);
      }
    });

    boolean isLegacy=ViewConfiguration.get(getActivity()).hasPermanentMenuKey();

    int layoutId=isLegacy || Utils.isSystemBarOnBottom(getActivity())
        ? R.layout.cwac_cam2_fragment_main
        : R.layout.cwac_cam2_fragment_main_alt;

    ViewGroup main=(ViewGroup)inflater.inflate(layoutId, container, false);

    main.addView(cv, 0);

    FlipView lens=(FlipView)main.findViewById(R.id.cwac_cam2_fragment_lens);

//    if (!ctrl.hasBothCameras()) {
      lens.setVisibility(View.GONE);
//    }

    return(main);
  }

  /**
   * @return the CameraController this fragment delegates to
   */
  public CameraController getController() {
    return(ctrl);
  }

  /**
   * Establishes the controller that this fragment delegates to
   *
   * @param ctrl the controller that this fragment delegates to
   */
  public void setController(CameraController ctrl) {
    this.ctrl=ctrl;
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraEngine.PictureTakenEvent event) {
    // TODO: figure out how to handle this, as I cannot quit
    // immediately, as otherwise the rest of the camera system
    // gets cranky

    // getContract().completeRequest();
    android.util.Log.e(getClass().getSimpleName(), "picture taken!");
  }

  private Contract getContract() {
    return((Contract)getActivity());
  }
}