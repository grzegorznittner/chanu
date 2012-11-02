package com.chanapps.four.data;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 10/30/12
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */


public class Captcha {

  public String Challenge;
  public String ImageUrl;
  public String AllHtml;
  //public DefaultHttpClient Client;

  public Captcha(String challenge, String imageUrl, String allHtml)
  {
    Challenge = challenge;
    ImageUrl = imageUrl;
    AllHtml = allHtml;
    //Client = client;
  }
}
