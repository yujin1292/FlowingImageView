# FlowingImageView
여러개의 이미지가 흘러가는듯한 이미지 뷰

## How to use

1. Data setting
    ~~~ kotiln 
            binding?.flowImageView?.setData(
                mutableListOf(image1, image2, ...),
                pixelPerSecond = ( pixelOf160dp / 1.5).toInt()  // 160dp / 1.5sec
            )
    ~~~ 

2. Attribute set 

- Inner image view size 
    - `app:item_height` , `app:item_width`
- Gap of the image
    - `app:gap_between_item` 




---

### Notice

- I used  `Glide` for image loading
