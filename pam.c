/**
 ** Copyright (C) 1989, 1991 by Jef Poskanzer.
 ** Copyright (C) 1997, 2000, 2002 by Greg Roelofs; based on an idea by
 **                                Stefan Schneider.
 ** (C) 2011 by Kornel Lesinski.
 **
 ** Permission to use, copy, modify, and distribute this software and its
 ** documentation for any purpose and without fee is hereby granted, provided
 ** that the above copyright notice appear in all copies and that both that
 ** copyright notice and this permission notice appear in supporting
 ** documentation.  This software is provided "as is" without express or
 ** implied warranty.
 */

#include <stdlib.h>
#include <string.h>
#include "pam.h"
#include "mempool.h"

/* libpam3.c - pam (portable alpha map) utility library part 3
 **
 ** Colormap routines.
 **
 ** Copyright (C) 1989, 1991 by Jef Poskanzer.
 ** Copyright (C) 1997 by Greg Roelofs.
 **
 ** Permission to use, copy, modify, and distribute this software and its
 ** documentation for any purpose and without fee is hereby granted, provided
 ** that the above copyright notice appear in all copies and that both that
 ** copyright notice and this permission notice appear in supporting
 ** documentation.  This software is provided "as is" without express or
 ** implied warranty.
 */

#define HASH_SIZE (((1<<19) - sizeof(struct acolorhash_table)) / sizeof(struct acolorhist_arr_head) - 1)

bool pam_computeacolorhash(struct acolorhash_table *acht, const rgb_pixel*const* apixels, unsigned int cols, unsigned int rows, const float *importance_map)
{
    const unsigned int maxacolors = acht->maxcolors, ignorebits = acht->ignorebits;
    const unsigned int channel_mask = 255>>ignorebits<<ignorebits;
    const unsigned int channel_hmask = (255>>ignorebits) ^ 0xFF;
    const unsigned int posterize_mask = channel_mask << 24 | channel_mask << 16 | channel_mask << 8 | channel_mask;
    const unsigned int posterize_high_mask = channel_hmask << 24 | channel_hmask << 16 | channel_hmask << 8 | channel_hmask;
    struct acolorhist_arr_head *const buckets = acht->buckets;

    unsigned int colors = acht->colors;

    const unsigned int stacksize = sizeof(acht->freestack)/sizeof(acht->freestack[0]);
    struct acolorhist_arr_item **freestack = acht->freestack;
    unsigned int freestackp=acht->freestackp;

    /* Go through the entire image, building a hash table of colors. */
    for(unsigned int row = 0; row < rows; ++row) {

        float boost=1.0;
        for(unsigned int col = 0; col < cols; ++col) {
            if (importance_map) {
                boost = 0.5f+*importance_map++;
            }

            // RGBA color is casted to long for easier hasing/comparisons
            union rgb_as_long px = {apixels[row][col]};
            unsigned long hash;
            if (!px.rgb.a) {
                // "dirty alpha" has different RGBA values that end up being the same fully transparent color
                px.l=0; hash=0;
            } else {
                // mask posterizes all 4 channels in one go
                px.l = (px.l & posterize_mask) | ((px.l & posterize_high_mask) >> (8-ignorebits));
                // fancier hashing algorithms didn't improve much
                hash = px.l % HASH_SIZE;
            }

            /* head of the hash function stores first 2 colors inline (achl->used = 1..2),
               to reduce number of allocations of achl->other_items.
             */
            struct acolorhist_arr_head *achl = &buckets[hash];
            if (achl->inline1.color.l == px.l && achl->used) {
                achl->inline1.perceptual_weight += boost;
                continue;
            }
            if (achl->used) {
                if (achl->used > 1) {
                    if (achl->inline2.color.l == px.l) {
                        achl->inline2.perceptual_weight += boost;
                        continue;
                    }
                    // other items are stored as an array (which gets reallocated if needed)
                    struct acolorhist_arr_item *other_items = achl->other_items;
                    unsigned int i = 0;
                    for (; i < achl->used-2; i++) {
                        if (other_items[i].color.l == px.l) {
                            other_items[i].perceptual_weight += boost;
                            goto continue_outer_loop;
                        }
                    }

                    // the array was allocated with spare items
                    if (i < achl->capacity) {
                        other_items[i] = (struct acolorhist_arr_item){
                            .color = px,
                            .perceptual_weight = boost,
                        };
                        achl->used++;
                        ++colors;
                        continue;
                    }

                    if (++colors > maxacolors) {
                        acht->colors = colors;
                        acht->freestackp = freestackp;
                        return false;
                    }

                    struct acolorhist_arr_item *new_items;
                    unsigned int capacity;
                    if (!other_items) { // there was no array previously, alloc "small" array
                        capacity = 8;
                        if (freestackp <= 0) {
                            new_items = mempool_new(&acht->mempool, sizeof(struct acolorhist_arr_item)*capacity);
                        } else {
                            // freestack stores previously freed (reallocated) arrays that can be reused
                            // (all pesimistically assumed to be capacity = 8)
                            new_items = freestack[--freestackp];
                        }
                    } else {
                        // simply reallocs and copies array to larger capacity
                        capacity = achl->capacity*2 + 16;
                        if (freestackp < stacksize-1) {
                            freestack[freestackp++] = other_items;
                        }
                        new_items = mempool_new(&acht->mempool, sizeof(struct acolorhist_arr_item)*capacity);
                        memcpy(new_items, other_items, sizeof(other_items[0])*achl->capacity);
                    }

                    achl->other_items = new_items;
                    achl->capacity = capacity;
                    new_items[i] = (struct acolorhist_arr_item){
                        .color = px,
                        .perceptual_weight = boost,
                    };
                    achl->used++;
                } else {
                    // these are elses for first checks whether first and second inline-stored colors are used
                    achl->inline2.color.l = px.l;
                    achl->inline2.perceptual_weight = boost;
                    achl->used = 2;
                    ++colors;
                }
            } else {
                achl->inline1.color.l = px.l;
                achl->inline1.perceptual_weight = boost;
                achl->used = 1;
                ++colors;
            }

            continue_outer_loop:;
        }

    }
    acht->colors = colors;
    acht->freestackp = freestackp;
    return true;
}

struct acolorhash_table *pam_allocacolorhash(unsigned int maxcolors, unsigned int ignorebits)
{
    mempool m = NULL;
    struct acolorhash_table *t = mempool_new(&m, sizeof(*t));
    t->buckets = mempool_new(&m, HASH_SIZE * sizeof(t->buckets[0]));
    t->mempool = m;
    t->maxcolors = maxcolors;
    t->ignorebits = ignorebits;
    return t;
}

#define PAM_ADD_TO_HIST(entry) { \
    hist->achv[j].acolor = to_f(entry.color.rgb); \
    hist->achv[j].adjusted_weight = hist->achv[j].perceptual_weight = entry.perceptual_weight; \
    ++j; \
    total_weight += entry.perceptual_weight; \
}

histogram *pam_acolorhashtoacolorhist(struct acolorhash_table *acht, double gamma)
{
    histogram *hist = malloc(sizeof(hist[0]));
    hist->achv = malloc(acht->colors * sizeof(hist->achv[0]));
    hist->size = acht->colors;

    to_f_set_gamma(gamma);

    double total_weight=0;
    for(unsigned int j=0, i=0; i < HASH_SIZE; ++i) {
        const struct acolorhist_arr_head *const achl = &acht->buckets[i];
        if (achl->used) {
            PAM_ADD_TO_HIST(achl->inline1);

            if (achl->used > 1) {
                PAM_ADD_TO_HIST(achl->inline2);

                for(unsigned int i=0; i < achl->used-2; i++) {
                    PAM_ADD_TO_HIST(achl->other_items[i]);
                }
            }
        }
    }

    hist->total_perceptual_weight = total_weight;
    return hist;
}


void pam_freeacolorhash(struct acolorhash_table *acht)
{
    mempool_free(acht->mempool);
}

void pam_freeacolorhist(histogram *hist)
{
    free(hist->achv);
    free(hist);
}

colormap *pam_colormap(unsigned int colors)
{
    colormap *map = malloc(sizeof(colormap));
    map->palette = calloc(colors, sizeof(map->palette[0]));
    map->subset_palette = NULL;
    map->colors = colors;
    map->palette_error = -1;
    return map;
}

void pam_freecolormap(colormap *c)
{
    if (c->subset_palette) pam_freecolormap(c->subset_palette);
    free(c->palette); free(c);
}

void to_f_set_gamma(double gamma)
{
    for(int i=0; i < 256; i++) {
        gamma_lut[i] = pow((double)i/255.0, internal_gamma/gamma);
    }
}

float gamma_lut[256];
