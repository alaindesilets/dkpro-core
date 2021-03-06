/*
 * Copyright 2011
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dkpro.core.api.frequency.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.MinMaxPriorityQueue;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * It is basically a mapping from samples (keys) to long values (counts).
 * 
 * <p>Suppose we want to record the number of occurrences of each word in a sentence, 
 * then this class can be used as follows:</p>
 * 
 * <blockquote><pre>
 * FrequencyDistribution&lt;String&gt; fd = new FrequencyDistribution&lt;String&gt;();
 * for (String word : "foo bar baz foo".split(" ")) {
 *     fd.inc(word);
 * }
 * System.out.println(fd.getCount("foo"));
 * </pre></blockquote>
 * 
 * <p>The last call to {@link FrequencyDistribution#getCount} will yield 2, because the
 * word "foo" has appeared twice in the given sequence of words.</p>
 * 
 * <p>This class was inspired by NLTK's <a
 * href="http://nltk.googlecode.com/svn/trunk/doc/api/nltk.probability.FreqDist-class.html">
 * FreqDist</a>.</p>
 * 
 * @param <T>
 *            the type of the samples
 * @see ConditionalFrequencyDistribution
 */
public class FrequencyDistribution<T>
    implements Serializable
{
    
    private static final long serialVersionUID = 150;

    private Object2LongOpenHashMap<T> freqDist;

    /** The total number of samples (accumulated count). */
    private long n;

    /** The maximum frequency in the distribution */
    private long maxFreq;

    /** The sample with the maximum frequency in the distribution */
    private T maxSample;

    /**
     * Creates a new empty {@link FrequencyDistribution}.
     */
    public FrequencyDistribution()
    {
        freqDist = new Object2LongOpenHashMap<T>();
        n = 0;
    }

    /**
     * Creates a new {@link FrequencyDistribution} prefilled with samples from an {@link Iterable}.
     * The count for each sample in the iterable is cumulatively increased by 1.
     * 
     * @param iterable
     *            the {@link Iterable} used to fill the {@link FrequencyDistribution}
     */
    public FrequencyDistribution(Iterable<T> iterable)
    {
        this();
        incAll(iterable);
    }

    /**
     * Indicates whether this distribution contains outcomes for a given <code>sample</code>.
     * 
     * @param sample
     *            the sample to look up
     * @return true if samples exist
     */
    public boolean contains(T sample)
    {
        return this.freqDist.containsKey(sample);
    }

    /**
     * Increments the count for a given <code>sample</code>.
     * 
     * @param sample
     *            the sample to increment the count for
     */
    public void inc(T sample)
    {
        addSample(sample, 1);
    }

    /**
     * Increments the count for each sample in a given {@link Iterable}.
     * 
     * @param iterable
     *            the samples used to increment the counts
     */

    public void incAll(Iterable<T> iterable)
    {
        for (T o : iterable) {
            addSample(o, 1);
        }
    }

    /**
     * Returns the total number of sample outcomes that have been recorded by this frequency
     * distribution. This is equal to the accumulated count of all samples (duplicates included).
     * 
     * @return the total number of sample outcomes
     */
    public long getN()
    {
        return n;
    }

    /**
     * Returns the total number of sample values (or bins) that have counts greater than zero. This
     * is equal to the accumulated counts of all distinct samples (duplicates excluded).
     * 
     * @return the total number of bins
     */
    public long getB()
    {
        return this.freqDist.size();
    }

    /**
     * Returns the count for a given <code>sample</code>. If no such samples have been recorded yet,
     * <code>0</code> will be returned.
     * 
     * @param sample
     *            the sample to get the count for
     * @return the count for a given sample
     */
    public long getCount(T sample)
    {
        if (freqDist.containsKey(sample)) {
            return freqDist.get(sample);
        }
        else {
            return 0;
        }
    }

    /**
     * Returns the {@link Set} of sample values (or bins) for which counts have been recorded.
     * 
     * @return the set of bins
     */
    @SuppressWarnings("unchecked")
    public Set<T> getKeys()
    {
        return this.freqDist.keySet();
    }

    /**
     * Increases the count for a given <code>sample</code>.
     * 
     * @param sample
     *            the sample to increase the count for
     * @param number
     *            the number to increase by
     */
    public void addSample(T sample, long number)
    {
        this.n = this.n + number;
        
        long sampleFreq = number;
        if (freqDist.containsKey(sample)) {
            sampleFreq = freqDist.get(sample) + number;
        }
        freqDist.put(sample, sampleFreq);
        
        if (sampleFreq > maxFreq) {
            maxFreq = sampleFreq;
            maxSample = sample;
        }
    }


    /**
     * Returns the highest frequency that is currently stored.
     * 
     * @return highest frequency that is currently stored.
     */
    public long getMaxFreq() {
        return maxFreq;
    }
    
    /**
     * Returns the sample which has currently the highest frequency. If there is more than one
     * sample which share the highest frequency, returns the one that was added first.
     * 
     * @return the sample which has currently the highest frequency
     */
    public T getSampleWithMaxFreq() {
        return maxSample;
    }
    
    
    public void save(File file)
            throws IOException
    {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        out.writeObject(freqDist);
        out.close();
    }

    public void load(File file)
            throws IOException, ClassNotFoundException
    {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        freqDist = (Object2LongOpenHashMap<T>) in.readObject();
        in.close();

        int samples = 0;
        
        LongIterator sampleIter = freqDist.values().iterator();

        // determine total frequency
        while (sampleIter.hasNext()) {
            long count = sampleIter.next();
            samples += count;
        }
        n = samples;
        
        // determine max sample
        for (T key : freqDist.keySet()) {
            Long freq = freqDist.get(key);
            if (freq > maxFreq) {
                maxFreq = freq;
                maxSample = key;
            }
        }
    }

    public void clear()
    {
        freqDist.clear();
        maxFreq = 0;
        maxSample = null;
        n = 0;
    }
    
    /**
     * Returns the n most frequent samples in the distribution. The ordering within in a group of
     * samples with the same frequency is undefined.
     * 
     * @param n
     *            the numer of most frequent samples to return.
     * @return the n most frequent samples in the distribution.
     */
    public List<T> getMostFrequentSamples(int n) {

        MinMaxPriorityQueue<TermFreqTuple<T>> topN = MinMaxPriorityQueue.maximumSize(n).create();

        for (T key : this.getKeys()) {
            topN.add(new TermFreqTuple<T>(key, this.getCount(key)));
        }
        
        List<T> topNList = new ArrayList<T>();
        while (!topN.isEmpty()) {
            topNList.add(topN.poll().getKey());
        }
        
        return topNList;
    }
    
    class ValueComparator
        implements Comparator<T>
    {
    
        Map<T, Long> base;
    
        public ValueComparator(Map<T, Long> base)
        {
            this.base = base;
        }
    
        @Override
        public int compare(T a, T b)
        {
    
            if (base.get(a) < base.get(b)) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }
}
