package com.imslpdroid;

import com.imslpdroid.data.Score;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class TestScoresActivity {


    private static String readAScorePage() throws IOException {
        URL url = new URL("http://imslp.org/wiki/Organ_Concerto_in_A_minor,_BWV_593_(Bach,_Johann_Sebastian)");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        StringWriter writer = new StringWriter();
        IOUtils.copy(conn.getInputStream(), writer);
        String pageHTML = writer.toString();
        return pageHTML;
    }

    @Test
    public void testFlululu() throws IOException {

        String html = readAScorePage();
        List<Score> scores = ScoresActivity.html2scores(html, "Bach, Johann Sebastian");
        System.out.println(String.format("found %d scores", scores.size()));
        for(Score s : scores) {
            System.out.println(s);
        }


    }

}
