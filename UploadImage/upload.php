<?php
// Path to move uploaded files
$target_path = dirname(__FILE__).'/uploads/';
$size = $_POST['size'];

if (!empty($_FILES)) {
    for ($x = 0; $x < $size; $x++) {
        try {
            $newname = date('YmdHis',time()).mt_rand().'.jpg';
            // Throws exception incase file is not being moved
            if (!move_uploaded_file($_FILES['image'.$x]['tmp_name'], $target_path .$newname)) {
                // make error flag true
                echo json_encode(array('status'=>'fail', 'message'=>'could not move file'));
            }
            // File successfully uploaded
            echo json_encode(array('status'=>'success', 'message'=>'File Uploaded'));
        } catch (Exception $e) {
            // Exception occurred. Make error flag true
            echo json_encode(array('status'=>'fail', 'message'=>$e->getMessage()));
        }
    }
} else {
    // File parameter is missing
    echo json_encode(array('status'=>'fail', 'message'=>'Not received any file'));
}