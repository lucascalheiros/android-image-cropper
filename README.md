# Android Image Cropper

This project is a simple and extensible image cropper.

It supports 2 modes for cropping an image, first by moving and scaling the crop area trough the view, and second by moving, scaling, and rotating the image with a fixed crop area.


## Usage

### Convenient usage with activity result api


Use ImageCropperContract to register for the crop activity result

` 

    private val imageResultRegister = registerForActivityResult(ImageCropperContract()) { uri ->
        uri ?: return@registerForActivityResult
        // do your stuff
    }
`

Launch the activity by passing the original image uri

`

    val uri = // get your bitmap file as uri
    imageResultRegister.launch(uri)
`

### AreaCropperView, ImageCropperView and CropAreaView

ImageCropperView is responsible for the move, scale, and rotation operations to the image.

CropAreaView is responsible for the move and scale operation to the crop area.

AreaCropperView is a convinience view that allows to use both ImageCropperView and CropAreaView altogether and change the cropping mode easily.

`

    <com.github.lucascalheiros.imagecropper.views.AreaCropperView
        android:id="@+id/areaCropperView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />


To set the image to be cropped in the view use:

    areaCropperView.setBitmap(bitmap)


Drag scale, and rotate as you please and  use the code below to retrieve the image: 

    areaCropperView.cropAreaToBitmap()


The default cropping mode is CropMode.MoveImage, if you want you can change to CropMode.MoveCrop at any time:

    areaCropperView.setCropMode(CropMode.MoveImage)
    // or
    areaCropperView.setCropMode(CropMode.MoveCrop)

If you want to change the crop area proportion and exhibition:

    areaCropperView.setProportion(proportion)
    areaCropperView.setVerticalDefaultCropBorder(valuePx)
    areaCropperView.setHorizontalDefaultCropBorder(valuePx)

`

