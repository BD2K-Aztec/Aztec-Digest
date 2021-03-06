package attributes.language;

import attributes.url.Url;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static attributes.Utilities.readJsonFromUrl;

public class Language {

    private List<String> m_language;

    public List<String> getLanguage() {  return this.m_language; }

    public Language(List<String> url_links, String name) throws Exception {
        this.m_language = extractProgrammingLang(url_links, name);
    }

    private List<String> extractProgrammingLang(List<String> url_links, String pdf_name) throws Exception {
        System.out.println(url_links);
        ArrayList<String> langlist = new ArrayList();

        String source_link = "";

        //consider edge case when bioconductor is involved
        for(String link: url_links) {
            if (link.toLowerCase().contains("bioconductor")) {
                langlist.add("R");
                return langlist;
            }
        }

        //iterate through all links to get github link
        for (int i=0; i < url_links.size(); i++){
            //perform GET request to get the github link -> for github repo name search
            if(url_links.get(i).contains("github")
                    || url_links.get(i).contains("sourceforge")
                    || url_links.get(i).contains("bitbucket")
                    || url_links.get(i).contains("bioconductor")){
                source_link = url_links.get(i);
                break;
            }
        }

        //if no github link found, iterate through all links to find github links
        if (source_link.equals("")){
            for(int i=0;i<url_links.size();i++){
                try{
                    String link = url_links.get(i);
                    if(link.contains("ContactInfo")){
                        link = link.split("Contact")[0];
                    }
                    String result = getHTML(link);

                    //pattern1 for github
                    if (link.contains("github")) {
                        String pattern = "href=\"(?=[^\"]*github)([^\"]*)";
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(result);
                        if (m.find()) {
                            source_link = m.group().split("\"")[1];
                            break;
                        }
                    } else if (link.contains("sourceforge")) {
                        //pattern2 for sourceforge
                        String pattern2 = "href=\"(?=[^\"]*sourceforge.net/projects)([^\"]*)";
                        Pattern r2 = Pattern.compile(pattern2);
                        Matcher m2 = r2.matcher(result);
                        if (m2.find()) {
                            source_link = m2.group().split("\"")[1];
                            break;
                        }
                    } else {
                        String pattern = "href=\"(?=[^\"]*github)([^\"]*)";
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(result);
                        if (m.find()) {
                            source_link = m.group().split("\"")[1];
                            break;
                        }
                        String pattern2 = "href=\"(?=[^\"]*sourceforge.net/projects)([^\"]*)";
                        Pattern r2 = Pattern.compile(pattern2);
                        Matcher m2 = r2.matcher(result);
                        if (m2.find()) {
                            source_link = m2.group().split("\"")[1];
                            break;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }

        //if it's a github personal page, find name and use github api to find m_language
        if (source_link.contains("github.io")) {
            String[] arr = source_link.split(".github.io");
            String name = arr[0];
            if(arr.length>1){
                name += arr[1];
            }
            langlist = accessLanguage(name);
            for (int i = 0; i < langlist.size(); i++) {
                langlist.set(i, langlist.get(i).trim());
            }
        }
        //if github link present, find programming m_language from github api
        else if (source_link.contains("github.com")) {
            if(source_link.split("github.com").length>1){
                //use Github api to access m_language info
                String name = source_link.split("github.com")[1];
                //clean the github name
                if(name.contains("ContactInfo")){
                    name = name.split("Contact")[0];
                }
                String[] arr = name.split("/");
                if(arr.length>=4){
                    name = arr[1]+"/"+arr[2];
                }
                langlist = accessLanguage(name);
                for (int i = 0; i < langlist.size(); i++) {
                    langlist.set(i, langlist.get(i).trim());
                }
            }
        }
        //sourceforge has SSL handshake error
        //if github_link contains sourceforge, find name
        else if(source_link.contains("sourceforge.net/projects")){

            if(source_link.contains("ContactInfo")){
                source_link = source_link.split("Contact")[0];
            }
            //use sourceforge api to access m_language info link
            String[] arr2 = source_link.split("sourceforge.net/projects");
            if(arr2.length>1){
                String name = source_link.split("sourceforge.net/projects")[1];
                String[] arr = name.split("/");
                if(arr.length>=3){
                    name = "/"+arr[1];
                }
                String access_link = "https://sourceforge.net/rest/p"+name;
                System.out.println(access_link);
                System.setProperty("https.protocols", "TLSv1");
                JSONObject github_page = readJsonFromUrl(access_link);
                JSONArray lang_arr = github_page.getJSONObject("categories").getJSONArray("m_language");
                if(lang_arr.length()>0){
                    String key = lang_arr.getJSONObject(0).getString("fullname");
                    langlist.add(key);
                }
            }
        }
        //if it's a sourceforge personal page, extract name and use api to find m_language
        else if(source_link.contains("sourceforge.net")){
            if(source_link.contains("ContactInfo")){
                source_link = source_link.split("Contact")[0];
            }
            String[] arr = source_link.split(".sourceforge.net");
            String name = "/"+arr[0];
            if(arr.length>1){
                name += arr[1];
            }
            String access_link = "https://sourceforge.net/rest/p"+name;
            System.out.println(access_link);
            System.setProperty("https.protocols", "TLSv1");
            JSONObject github_page = readJsonFromUrl(access_link);
            JSONArray lang_arr = github_page.getJSONObject("categories").getJSONArray("m_language");
            if(lang_arr.length()>0){
                String key = lang_arr.getJSONObject(0).getString("fullname");
                langlist.add(key);
            }

        }
        else if(source_link.contains("bioconductor")){
            langlist.add("R");
        }
        else if (source_link.contains("bitbucket.org")) {
            if(source_link.split("bitbucket.org/").length>1){
                String name = source_link.split("bitbucket.org/")[1];
                String access_link = "https://api.bitbucket.org/2.0/repositories/"+name;
                JSONObject github_page = readJsonFromUrl(access_link);
                String key = github_page.getString("m_language");
                langlist.add(key);
            }
        }

        return langlist;
    }




    //helper function: get HTML content
    private static String getHTML(String urlToRead) throws Exception {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("User-Agent", "Mozilla");
            conn.addRequestProperty("Referer", "google.com");

            boolean redirect = false;
            // normally, 3xx is redirect
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;
            }

            if (redirect) {

                // get redirect url from "location" header field
                String newUrl = conn.getHeaderField("Location");

                // get the cookie if need, for login
                String cookies = conn.getHeaderField("Set-Cookie");

                // open the new connnection again
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Cookie", cookies);
                conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                conn.addRequestProperty("User-Agent", "Mozilla");
                conn.addRequestProperty("Referer", "google.com");

            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer html = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                html.append(inputLine);
            }
            in.close();

            return html.toString();
        }
        catch (Exception e){
            return "";
        }
    }

    private ArrayList<String> accessLanguage(String name) throws IOException {
        System.out.println(name);
        ArrayList<String> lan = new ArrayList<>();
        String access_link = "https://api.github.com/search/repositories?q="+name+"%20in:name&sort=stars&order=desc";
        JSONObject github_page = readJsonFromUrl(access_link);
        System.out.println(github_page);
        String new_page_info = "";
        if (github_page.getInt("total_count")!=0){
            new_page_info = github_page.getJSONArray("items").getJSONObject(0).getString("languages_url");
        }
        else {
            return lan;
        }

        //access git m_language info
        try{
            JSONObject lang_info = readJsonFromUrl(new_page_info);
            Iterator<String> keys = lang_info.keys();
            String prev_key = (String)keys.next(); // First key in your json object
            int max = lang_info.getInt(prev_key);
            lan.add(prev_key);
            while (keys.hasNext()) {
                String key = (String)keys.next(); // First key in your json object
                int num = lang_info.getInt(key);
                if (num > max){
                    lan.remove(prev_key);
                    lan.add(key);
                    max = num;
                    prev_key = key;
                }
            }
        }
        catch (Exception e){

        }
        return lan;
    }
}

