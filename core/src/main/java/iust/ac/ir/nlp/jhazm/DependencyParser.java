package iust.ac.ir.nlp.jhazm;

import com.infomancers.collections.yield.Yielder;
import edu.stanford.nlp.ling.TaggedWord;
import ir.ac.iust.text.utils.FileHandler;
import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 *
 * @author Mojtaba Khallash
 */
public class DependencyParser {
    public static DependencyParser instance;
    public POSTagger tagger;
    private SentenceTokenizer sentenceTokenizer;
    private WordTokenizer wordTokenizer;
    private Normalizer normalizer;
    private Lemmatizer lemmatizer;
    private String modelFile;
    private ConcurrentMaltParserModel model;

    public DependencyParser() {
        this(null, null, "resources/langModel.mco");
    }

    public DependencyParser(POSTagger tagger, Lemmatizer lemmatizer, String modelFile) {
        this.tagger = tagger;
        this.lemmatizer = lemmatizer;
        this.modelFile = modelFile;
    }

    public static DependencyParser i() {
        if (instance != null) return instance;
        instance = new DependencyParser();
        return instance;
    }

    public SentenceTokenizer getSentenceTokenizer() {
        if (sentenceTokenizer == null)
            sentenceTokenizer = new SentenceTokenizer();
        return sentenceTokenizer;
    }

    public void setSentenceTokenizer(SentenceTokenizer value) {
        this.sentenceTokenizer = value;
    }

    public WordTokenizer getWordTokenizer() throws IOException {
        if (wordTokenizer == null)
            wordTokenizer = new WordTokenizer();
        return wordTokenizer;
    }

    public void setWordTokenizer(WordTokenizer value) {
        this.wordTokenizer = value;
    }

    public Normalizer getNormalizer() {
        return normalizer;
    }

    public void setNormalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
    }

    public Lemmatizer getLemmatizer() {
        return lemmatizer;
    }

    public void setLemmatizer(Lemmatizer lemmatizer) {
        this.lemmatizer = lemmatizer;
    }

    public POSTagger getTagger() throws IOException {
        if (tagger == null)
            tagger = new POSTagger();
        return tagger;
    }

    public void setTagger(POSTagger value) {
        this.tagger = value;
    }

    private ConcurrentMaltParserModel getModel()
            throws IOException,
            MaltChainedException {
        if (model == null) {
            URL maltModelURL = FileHandler.getPath(this.modelFile).toUri().toURL();
            this.model = ConcurrentMaltParserService.initializeParserModel(maltModelURL);
        }
        return model;
    }

    // Gets list of raw text
    public Iterable<ConcurrentDependencyGraph> RawParse(String text) 
            throws IOException {
        if (this.normalizer != null)
            text = this.normalizer.Run(text);
        return RawParses(getSentenceTokenizer().Tokenize(text));
    }

    // Gets list of raw sentences
    public Iterable<ConcurrentDependencyGraph> RawParses(List<String> sentences) 
            throws IOException {
        return new YieldParsedSentence(sentences);
    }

    public ConcurrentDependencyGraph RawParse(List<TaggedWord> sentence)
            throws IOException,
            MaltChainedException {
        String[] conll = new String[sentence.size()];
        for (int i = 0; i < sentence.size(); i++) {
            TaggedWord taggedWord = sentence.get(i);
            String word = taggedWord.word();
            String Lemma = "_";
            if (this.lemmatizer != null)
                Lemma = this.lemmatizer.lemmatize(word);
            String pos = taggedWord.tag();

            conll[i] = String.format("%s\t%s\t%s\t%s\t%s\t%s",
                    i + 1, word, Lemma, pos, pos, "_");
        }
        return Parse(conll);
    }

    public ConcurrentDependencyGraph Parse(String[] conllSentence)
            throws IOException,
            MaltChainedException {
        return this.getModel().parse(conllSentence);
    }

    class YieldParsedSentence extends Yielder<ConcurrentDependencyGraph> {
        private final List<String> sentences;
        private int index;

        public YieldParsedSentence(List<String> sentences) {
            this.sentences = sentences;
            index = -1;
        }

        @Override
        protected void yieldNextCore() {
            try {
                index++;
                if (index < sentences.size()) {
                    String sentence = sentences.get(index);
                    List<String> words = getWordTokenizer().Tokenize(sentence);
                    yieldReturn(RawParse(getTagger().batchTag(words)));
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}