/*
 * The copyright in this software is being made available under the 2-clauses
 * BSD License, included below. This software may be subject to other third
 * party and contributor rights, including patent rights, and no such rights
 * are granted under this license.
 *
 * Copyright (c) 2002-2014, Universite catholique de Louvain (UCL), Belgium
 * Copyright (c) 2002-2014, Professor Benoit Macq
 * Copyright (c) 2001-2003, David Janssens
 * Copyright (c) 2002-2003, Yannick Verschueren
 * Copyright (c) 2003-2007, Francois-Olivier Devaux
 * Copyright (c) 2003-2014, Antonin Descampe
 * Copyright (c) 2005, Herve Drolon, FreeImage Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 */
#ifndef OPJ_BIO_H
#define OPJ_BIO_H

#include <stddef.h> /* ptrdiff_t */

/**
   @file bio.h
   @brief Implementation of an individual bit input-output (BIO)

   The functions in BIO.C have for goal to realize an individual bit input - output.
 */

/** @defgroup BIO BIO - Individual bit input-output stream */
/*@{*/

/**
   Individual bit input-output stream (BIO)
 */
typedef struct opj_bio {
	/** pointer to the start of the buffer */
	uint8 * start;
	/** pointer to the end of the buffer */
	uint8 * end;
	/** pointer to the present position in the buffer */
	uint8 * bp;
	/** temporary place where each byte is read or written */
	uint32_t buf;
	/** coder : number of bits free to write. decoder : number of bits read */
	uint32_t ct;
} opj_bio_t;

/** @name Exported functions */
/*@{*/
/**
   Create a new BIO handle
   @return Returns a new BIO handle if successful, returns NULL otherwise
 */
opj_bio_t* opj_bio_create(void);
/**
   Destroy a previously created BIO handle
   @param bio BIO handle to destroy
 */
void opj_bio_destroy(opj_bio_t * bio);
/**
   Number of bytes written.
   @param bio BIO handle
   @return Returns the number of bytes written
 */
ptrdiff_t opj_bio_numbytes(opj_bio_t * bio);
/**
   Init encoder
   @param bio BIO handle
   @param bp Output buffer
   @param len Output buffer length
 */
void opj_bio_init_enc(opj_bio_t * bio, uint8 * bp, uint32_t len);
/**
   Init decoder
   @param bio BIO handle
   @param bp Input buffer
   @param len Input buffer length
 */
void opj_bio_init_dec(opj_bio_t * bio, uint8 * bp, uint32_t len);
/**
   Write bits
   @param bio BIO handle
   @param v Value of bits
   @param n Number of bits to write
 */
void opj_bio_write(opj_bio_t * bio, uint32_t v, uint32_t n);
/**
   Read bits
   @param bio BIO handle
   @param n Number of bits to read
   @return Returns the corresponding read number
 */
uint32_t opj_bio_read(opj_bio_t * bio, uint32_t n);
/**
   Flush bits
   @param bio BIO handle
   @return Returns TRUE if successful, returns FALSE otherwise
 */
boolint opj_bio_flush(opj_bio_t * bio);
/**
   Passes the ending bits (coming from flushing)
   @param bio BIO handle
   @return Returns TRUE if successful, returns FALSE otherwise
 */
boolint opj_bio_inalign(opj_bio_t * bio);
/*@}*/
/*@}*/

#endif /* OPJ_BIO_H */
