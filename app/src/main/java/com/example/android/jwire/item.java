package com.example.android.jwire;


import android.os.Parcel;
import android.os.Parcelable;

public class item implements Parcelable {

    String itemTitle;
    String itemSection;
    String itemDateTime;
    String itemUrl;
    String itemAuthor;

    public void setItem(String item_title, String item_section, String item_Date_time, String item_url, String item_author) {

        itemTitle = item_title;
        itemSection = item_section;
        itemDateTime = item_Date_time;
        itemUrl = item_url;
        itemAuthor = item_author;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.itemTitle);
        dest.writeString(this.itemSection);
        dest.writeString(this.itemDateTime);
        dest.writeString(this.itemUrl);
        dest.writeString(this.itemAuthor);
    }

    public item() {
    }

    protected item(Parcel in) {
        this.itemTitle = in.readString();
        this.itemSection = in.readString();
        this.itemDateTime = in.readString();
        this.itemUrl = in.readString();
        this.itemAuthor = in.readString();
    }

    public static final Parcelable.Creator<item> CREATOR = new Parcelable.Creator<item>() {
        @Override
        public item createFromParcel(Parcel source) {
            return new item(source);
        }

        @Override
        public item[] newArray(int size) {
            return new item[size];
        }
    };
}