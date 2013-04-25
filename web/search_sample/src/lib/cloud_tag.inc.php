<?php

class cloud_tag
{
    // constructor. init our variables
    function cloud_tag()
    {
        $this->tag_label = "tag";
        $this->tag_sizes = 7;
    }

    // set the label for the css class
    function set_label($label) {$this->tag_label = $label;}

    // set the number of buckets
    function set_tagsizes($sizes) {$this->tag_sizes = $sizes;}

    // create a cloud tag
    function make_cloud($tags)
    {

        usort($tags, array($this,'tag_asort'));
        if(count($tags) == 0) return $tags;

        // Start with the sorted list of tags and divide by the number of font sizes (buckets).
        // Then proceed to put an even number of tags into each bucket. The only restriction is
        // that tags of the same count can't span 2 buckets, so some buckets may have more tags
        // than others. Because of this, the sorted list of remaining tags is divided by the
        // remaining 'buckets' to evenly distribute the remainder of the tags and to fill as
        // many 'buckets' as possible up to the largest font size.

        $total_tags = count($tags);
        $min_tags = $total_tags / $this->tag_sizes;

        $bucket_count = 1;
        $bucket_items = 0;
        $tags_set = 0;
        foreach($tags as $key => $tag)
        {
            $tag_count = $tag["tag_count"];

            // If we've met the minimum number of tags for this class and the current tag
            // does not equal the last tag, we can proceed to the next class.

            if(($bucket_items >= $min_tags) and $last_count != $tag_count and $bucket_count < $this->tag_sizes)
            {
                $bucket_count++;
                $bucket_items = 0;

                // Calculate a new minimum number of tags for the remaining classes.
                $remaining_tags = $total_tags - $tags_set;
                $min_tags = $remaining_tags / $bucket_count;
            }

            // Set the tag to the current class.
            $tags[$key]["tag_class"] = $this->tag_label.$bucket_count;
            $bucket_items++;
            $tags_set++;

            $last_count = $tag_count;
        }

        usort($tags, array($this,'tag_alphasort'));

        return $tags;
    }

/*-------------------------------------------------------
 * internal-use-only below here
 *-------------------------------------------------------*/

    // sorts a list of tags by their count ascending.
    function tag_asort($tag1, $tag2)
    {
        if($tag1["tag_count"] == $tag2["tag_count"]) return 0;
        return ($tag1["tag_count"] < $tag2["tag_count"]) ? -1 : 1;
    }

    // sorts a list of tags alphabetically by tag_name
    function tag_alphasort($tag1, $tag2)
    {
        if($tag1["tag_name"] == $tag2["tag_name"]) return 0;
        return ($tag1["tag_name"] < $tag2["tag_name"]) ? -1 : 1;
    }

/*-------------------------------------------------------
 * member variables
 * -------------------------------------------------------*/

var $tag_label; // the css base class name
var $tag_sizes; // number of buckets (font sizes)
}


?>
