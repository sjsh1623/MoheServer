--
-- PostgreSQL database dump
--

\restrict lWDrOvKsd7aqrQQFJkpEr6kyLab7ZWe4VSxYWhKOyxQRLsdK8tYkwFWnGxwgbRB

-- Dumped from database version 15.14 (Debian 15.14-1.pgdg12+1)
-- Dumped by pg_dump version 15.14 (Debian 15.14-1.pgdg12+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE IF EXISTS mohe_db;
--
-- Name: mohe_db; Type: DATABASE; Schema: -; Owner: -
--

CREATE DATABASE mohe_db WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.utf8';


\unrestrict lWDrOvKsd7aqrQQFJkpEr6kyLab7ZWe4VSxYWhKOyxQRLsdK8tYkwFWnGxwgbRB
\connect mohe_db
\restrict lWDrOvKsd7aqrQQFJkpEr6kyLab7ZWe4VSxYWhKOyxQRLsdK8tYkwFWnGxwgbRB

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activities (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    activity_type character varying(50) NOT NULL,
    place_id bigint,
    "timestamp" timestamp without time zone DEFAULT now()
);


--
-- Name: activities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.activities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: activities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.activities_id_seq OWNED BY public.activities.id;


--
-- Name: batch_job_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


--
-- Name: batch_job_execution_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


--
-- Name: batch_job_execution_job_execution_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_execution_job_execution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_job_execution_job_execution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batch_job_execution_job_execution_id_seq OWNED BY public.batch_job_execution.job_execution_id;


--
-- Name: batch_job_execution_params; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    parameter_name character varying(100) NOT NULL,
    parameter_type character varying(100) NOT NULL,
    parameter_value character varying(2500),
    identifying character(1) NOT NULL
);


--
-- Name: batch_job_execution_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_job_instance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);


--
-- Name: batch_job_instance_job_instance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_instance_job_instance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_job_instance_job_instance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batch_job_instance_job_instance_id_seq OWNED BY public.batch_job_instance.job_instance_id;


--
-- Name: batch_job_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_step_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


--
-- Name: batch_step_execution_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


--
-- Name: batch_step_execution_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_step_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_step_execution_step_execution_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_step_execution_step_execution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_step_execution_step_execution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batch_step_execution_step_execution_id_seq OWNED BY public.batch_step_execution.step_execution_id;


--
-- Name: bookmarks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bookmarks (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    place_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: bookmarks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bookmarks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bookmarks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bookmarks_id_seq OWNED BY public.bookmarks.id;


--
-- Name: email_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verifications (
    id bigint NOT NULL,
    code character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    issued_at timestamp(6) with time zone,
    success boolean NOT NULL,
    verified_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: email_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.email_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: email_verifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.email_verifications_id_seq OWNED BY public.email_verifications.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: keyword_catalog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.keyword_catalog (
    id bigint NOT NULL,
    category character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    keyword character varying(100) NOT NULL,
    mbti_weights jsonb,
    updated_at timestamp(6) without time zone
);


--
-- Name: keyword_catalog_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.keyword_catalog_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: keyword_catalog_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.keyword_catalog_id_seq OWNED BY public.keyword_catalog.id;


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(255) NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp(6) with time zone NOT NULL,
    used boolean
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: place_business_hours; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_business_hours (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    day_of_week character varying(10),
    open time without time zone,
    close time without time zone,
    description text,
    is_operating boolean DEFAULT true,
    last_order_minutes integer
);


--
-- Name: place_business_hours_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_business_hours_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_business_hours_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_business_hours_id_seq OWNED BY public.place_business_hours.id;


--
-- Name: place_description_vectors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_description_vectors (
    id bigint NOT NULL,
    combined_attributes_text text,
    created_at timestamp(6) with time zone,
    description_vector text NOT NULL,
    extraction_prompt_hash character varying(255),
    extraction_source character varying(255) NOT NULL,
    model_name character varying(255) NOT NULL,
    model_version character varying(255),
    raw_description_text text NOT NULL,
    selected_keywords text NOT NULL,
    updated_at timestamp(6) with time zone,
    place_id bigint NOT NULL
);


--
-- Name: place_description_vectors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_description_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_description_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_description_vectors_id_seq OWNED BY public.place_description_vectors.id;


--
-- Name: place_descriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_descriptions (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    original_description text,
    ai_summary text,
    ollama_description text,
    search_query character varying(500),
    updated_at timestamp without time zone DEFAULT now()
);


--
-- Name: place_descriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_descriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_descriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_descriptions_id_seq OWNED BY public.place_descriptions.id;


--
-- Name: place_external_raw; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_external_raw (
    id bigint NOT NULL,
    external_id character varying(255) NOT NULL,
    fetched_at timestamp(6) without time zone,
    payload jsonb NOT NULL,
    place_id bigint,
    source character varying(50) NOT NULL
);


--
-- Name: place_external_raw_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_external_raw_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_external_raw_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_external_raw_id_seq OWNED BY public.place_external_raw.id;


--
-- Name: place_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_images (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    url text NOT NULL,
    order_index integer,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: place_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_images_id_seq OWNED BY public.place_images.id;


--
-- Name: place_mbti_descriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_mbti_descriptions (
    id bigint NOT NULL,
    description text NOT NULL,
    mbti character varying(4) NOT NULL,
    model character varying(100),
    place_id bigint NOT NULL,
    prompt_hash character varying(64) NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: place_mbti_descriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_mbti_descriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_mbti_descriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_mbti_descriptions_id_seq OWNED BY public.place_mbti_descriptions.id;


--
-- Name: place_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_reviews (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    review_text text,
    order_index integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: place_reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_reviews_id_seq OWNED BY public.place_reviews.id;


--
-- Name: place_similarity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_similarity (
    place_id1 bigint NOT NULL,
    place_id2 bigint NOT NULL,
    co_users integer,
    cosine_bin numeric(5,4),
    jaccard numeric(5,4),
    updated_at timestamp(6) without time zone
);


--
-- Name: place_similarity_topk; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_similarity_topk (
    neighbor_place_id bigint NOT NULL,
    place_id bigint NOT NULL,
    co_users integer,
    cosine_bin numeric(5,4),
    jaccard numeric(5,4),
    rank smallint NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: place_sns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_sns (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    platform character varying(50) NOT NULL,
    url text NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: place_sns_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_sns_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_sns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_sns_id_seq OWNED BY public.place_sns.id;


--
-- Name: places; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.places (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    latitude numeric(10,8),
    longitude numeric(11,8),
    road_address text,
    website_url text,
    rating numeric(38,2) DEFAULT 0.0,
    review_count integer DEFAULT 0,
    category character varying(100)[],
    keyword character varying(255)[],
    keyword_vector text,
    opening_hours jsonb,
    parking_available boolean DEFAULT false,
    pet_friendly boolean DEFAULT false,
    ready boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    crawler_found boolean
);


--
-- Name: COLUMN places.crawler_found; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.places.crawler_found IS 'Indicates whether the place was successfully found by the crawler (true=found, false=not found, null=not yet processed)';


--
-- Name: places_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.places_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: places_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.places_id_seq OWNED BY public.places.id;


--
-- Name: preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preferences (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    pref_key character varying(255) NOT NULL,
    pref_value character varying(255),
    user_id bigint NOT NULL
);


--
-- Name: preferences_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.preferences_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: preferences_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.preferences_id_seq OWNED BY public.preferences.id;


--
-- Name: prompts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prompts (
    id bigint NOT NULL,
    content character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    place_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: prompts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.prompts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: prompts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.prompts_id_seq OWNED BY public.prompts.id;


--
-- Name: recent_views; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recent_views (
    id bigint NOT NULL,
    viewed_at timestamp(6) with time zone,
    place_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: recent_views_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recent_views_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recent_views_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recent_views_id_seq OWNED BY public.recent_views.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(255) NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp(6) with time zone NOT NULL,
    is_revoked boolean
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: temp_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.temp_users (
    id character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    otp character varying(10) NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    verified boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp(6) with time zone NOT NULL,
    nickname character varying(255),
    password_hash character varying(255),
    terms_agreed boolean,
    verification_code character varying(255) NOT NULL
);


--
-- Name: temp_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.temp_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: temp_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.temp_users_id_seq OWNED BY public.temp_users.id;


--
-- Name: terms_agreements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terms_agreements (
    id bigint NOT NULL,
    agreed boolean NOT NULL,
    agreed_at timestamp(6) with time zone,
    terms_code character varying(255) NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: terms_agreements_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.terms_agreements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: terms_agreements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.terms_agreements_id_seq OWNED BY public.terms_agreements.id;


--
-- Name: user_preference_vectors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_preference_vectors (
    id bigint NOT NULL,
    combined_preferences_text text,
    created_at timestamp(6) with time zone,
    extraction_prompt_hash character varying(255),
    extraction_source character varying(255) NOT NULL,
    model_name character varying(255) NOT NULL,
    model_version character varying(255),
    preference_vector text NOT NULL,
    raw_profile_text text NOT NULL,
    selected_keywords text NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: user_preference_vectors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_preference_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_preference_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_preference_vectors_id_seq OWNED BY public.user_preference_vectors.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    nickname character varying(50) NOT NULL,
    mbti character varying(4),
    age_range character varying(20),
    transportation_method character varying(50),
    space_preferences text[],
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_onboarding_completed boolean,
    last_login_at timestamp(6) with time zone,
    password_hash character varying(255) NOT NULL,
    profile_image_url character varying(255),
    transportation character varying(255)
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vector_similarities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vector_similarities (
    place_id bigint NOT NULL,
    user_id bigint NOT NULL,
    calculated_at timestamp(6) with time zone,
    common_keywords integer NOT NULL,
    cosine_similarity numeric(5,4) NOT NULL,
    euclidean_distance numeric(8,4) NOT NULL,
    jaccard_similarity numeric(5,4),
    keyword_overlap_ratio numeric(3,2),
    mbti_boost_factor numeric(3,2),
    place_vector_version bigint,
    user_vector_version bigint,
    weighted_similarity numeric(5,4) NOT NULL
);


--
-- Name: activities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activities ALTER COLUMN id SET DEFAULT nextval('public.activities_id_seq'::regclass);


--
-- Name: batch_job_execution job_execution_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch_job_execution ALTER COLUMN job_execution_id SET DEFAULT nextval('public.batch_job_execution_job_execution_id_seq'::regclass);


--
-- Name: batch_job_instance job_instance_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch_job_instance ALTER COLUMN job_instance_id SET DEFAULT nextval('public.batch_job_instance_job_instance_id_seq'::regclass);


--
-- Name: batch_step_execution step_execution_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch_step_execution ALTER COLUMN step_execution_id SET DEFAULT nextval('public.batch_step_execution_step_execution_id_seq'::regclass);


--
-- Name: bookmarks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookmarks ALTER COLUMN id SET DEFAULT nextval('public.bookmarks_id_seq'::regclass);


--
-- Name: email_verifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications ALTER COLUMN id SET DEFAULT nextval('public.email_verifications_id_seq'::regclass);


--
-- Name: keyword_catalog id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keyword_catalog ALTER COLUMN id SET DEFAULT nextval('public.keyword_catalog_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: place_business_hours id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_business_hours ALTER COLUMN id SET DEFAULT nextval('public.place_business_hours_id_seq'::regclass);


--
-- Name: place_description_vectors id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_description_vectors ALTER COLUMN id SET DEFAULT nextval('public.place_description_vectors_id_seq'::regclass);


--
-- Name: place_descriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_descriptions ALTER COLUMN id SET DEFAULT nextval('public.place_descriptions_id_seq'::regclass);


--
-- Name: place_external_raw id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_external_raw ALTER COLUMN id SET DEFAULT nextval('public.place_external_raw_id_seq'::regclass);


--
-- Name: place_images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_images ALTER COLUMN id SET DEFAULT nextval('public.place_images_id_seq'::regclass);


--
-- Name: place_mbti_descriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_mbti_descriptions ALTER COLUMN id SET DEFAULT nextval('public.place_mbti_descriptions_id_seq'::regclass);


--
-- Name: place_reviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_reviews ALTER COLUMN id SET DEFAULT nextval('public.place_reviews_id_seq'::regclass);


--
-- Name: place_sns id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_sns ALTER COLUMN id SET DEFAULT nextval('public.place_sns_id_seq'::regclass);


--
-- Name: places id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.places ALTER COLUMN id SET DEFAULT nextval('public.places_id_seq'::regclass);


--
-- Name: preferences id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferences ALTER COLUMN id SET DEFAULT nextval('public.preferences_id_seq'::regclass);


--
-- Name: prompts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prompts ALTER COLUMN id SET DEFAULT nextval('public.prompts_id_seq'::regclass);


--
-- Name: recent_views id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recent_views ALTER COLUMN id SET DEFAULT nextval('public.recent_views_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: temp_users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.temp_users ALTER COLUMN id SET DEFAULT nextval('public.temp_users_id_seq'::regclass);


--
-- Name: terms_agreements id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms_agreements ALTER COLUMN id SET DEFAULT nextval('public.terms_agreements_id_seq'::regclass);


--
-- Name: user_preference_vectors id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_preference_vectors ALTER COLUMN id SET DEFAULT nextval('public.user_preference_vectors_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: activities; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.activities (id, user_id, activity_type, place_id, "timestamp") FROM stdin;
\.


--
-- Data for Name: batch_job_execution; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch_job_execution (job_execution_id, version, job_instance_id, create_time, start_time, end_time, status, exit_code, exit_message, last_updated) FROM stdin;
1	2	1	2025-10-08 14:32:15.666945	2025-10-08 14:32:15.697127	2025-10-08 14:32:31.302903	COMPLETED	COMPLETED		2025-10-08 14:32:31.303034
16	2	16	2025-10-09 03:32:18.103748	2025-10-09 03:32:18.108791	2025-10-09 03:33:33.414182	COMPLETED	COMPLETED		2025-10-09 03:33:33.414215
2	2	2	2025-10-08 14:32:47.021624	2025-10-08 14:32:47.025847	2025-10-08 14:35:36.202358	COMPLETED	COMPLETED		2025-10-08 14:35:36.202447
3	2	3	2025-10-08 14:36:43.330395	2025-10-08 14:36:43.334506	2025-10-08 14:36:53.629577	COMPLETED	COMPLETED		2025-10-08 14:36:53.629641
25	1	25	2025-10-09 14:42:26.032043	2025-10-09 14:42:26.034803	\N	STARTED	UNKNOWN		2025-10-09 14:42:26.035341
4	2	4	2025-10-08 14:51:06.259526	2025-10-08 14:51:06.295085	2025-10-08 14:51:20.992196	COMPLETED	COMPLETED		2025-10-08 14:51:20.992331
17	2	17	2025-10-09 03:38:24.231536	2025-10-09 03:38:24.24326	2025-10-09 03:53:53.649694	STOPPED	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 03:53:53.649774
5	2	5	2025-10-08 14:55:36.574313	2025-10-08 14:55:36.597236	2025-10-08 14:56:24.354429	COMPLETED	COMPLETED		2025-10-08 14:56:24.354634
6	2	6	2025-10-08 17:08:41.218521	2025-10-08 17:08:41.243845	2025-10-08 17:09:20.51514	COMPLETED	COMPLETED		2025-10-08 17:09:20.515257
7	2	7	2025-10-08 17:13:43.874819	2025-10-08 17:13:43.889827	2025-10-08 17:14:23.804052	COMPLETED	COMPLETED		2025-10-08 17:14:23.804249
18	2	18	2025-10-09 03:55:41.239701	2025-10-09 03:55:41.258072	2025-10-09 04:17:20.233303	STOPPED	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 04:17:20.233397
8	2	8	2025-10-08 17:18:32.433122	2025-10-08 17:18:32.454101	2025-10-08 17:19:10.997471	COMPLETED	COMPLETED		2025-10-08 17:19:10.997579
9	2	9	2025-10-08 17:23:08.584158	2025-10-08 17:23:08.60783	2025-10-08 17:23:49.242438	COMPLETED	COMPLETED		2025-10-08 17:23:49.242543
26	1	26	2025-10-09 14:42:59.084663	2025-10-09 14:42:59.08724	\N	STARTED	UNKNOWN		2025-10-09 14:42:59.087802
10	2	10	2025-10-08 17:25:28.346812	2025-10-08 17:25:28.366218	2025-10-08 17:26:07.105086	COMPLETED	COMPLETED		2025-10-08 17:26:07.105193
19	2	19	2025-10-09 10:26:30.702626	2025-10-09 10:26:30.722562	2025-10-09 12:20:16.579702	COMPLETED	COMPLETED		2025-10-09 12:20:16.579797
11	2	11	2025-10-08 17:28:26.466235	2025-10-08 17:28:26.486358	2025-10-08 17:30:28.469015	COMPLETED	COMPLETED		2025-10-08 17:30:28.469138
12	2	12	2025-10-09 02:15:34.642203	2025-10-09 02:15:34.654258	2025-10-09 02:15:34.814311	FAILED	FAILED	org.springframework.web.reactive.function.client.WebClientRequestException: Connection refused: localhost/127.0.0.1:4000\n\tat org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:136)\n\tSuppressed: The stacktrace has been enhanced by Reactor, refer to additional information below: \nError has been observed at the following site(s):\n\t*__checkpoint ⇢ Request to POST http://localhost:4000/ [DefaultWebClient]\nOriginal Stack Trace:\n\t\tat org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:136)\n\t\tat reactor.core.publisher.MonoErrorSupplied.subscribe(MonoErrorSupplied.java:55)\n\t\tat reactor.core.publisher.Mono.subscribe(Mono.java:4512)\n\t\tat reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:103)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222)\n\t\tat reactor.core.publisher.MonoNext$NextSubscriber.onError(MonoNext.java:93)\n\t\tat reactor.core.publisher.MonoFlatMapMany$FlatMapManyMain.onError(MonoFlatMapMany.java:204)\n\t\tat reactor.core.publisher.SerializedSubscriber.onError(SerializedSubscriber.java:124)\n\t\tat reactor.core.publisher.FluxRetryWhen$RetryWhenMainSubscriber.whenError(FluxRetryWhen.java:228)\n\t\tat reactor.core.publisher.FluxRetryWhen$RetryWhenOtherSubscriber.onError(FluxRetryWhen.java:278)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onError(FluxContextWrite.java:121)\n\t\tat reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.maybeOnError(FluxConcatMapNoPrefetch.java:326)\n\t\tat reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.onNext(FluxConcatMapNoPrefetch.java:211)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)\n\t\tat reactor.core.publisher.SinkManyEmitterProcessor.drain(SinkManyEmitterProcessor.java:476)\n\t\tat reactor.core.publisher.SinkManyEmitterProcessor$EmitterInner.drainParent(SinkManyEmitterProcessor.java:620)\n\t\tat reactor.core.publisher.FluxPublish$PubSubInner.request(FluxPublish.java:874)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.request(FluxContextWrite.java:136)\n\t\tat reactor.core.publisher.FluxConcatMapNoPref	2025-10-09 02:15:34.81435
13	2	13	2025-10-09 02:17:03.464888	2025-10-09 02:17:03.476845	2025-10-09 02:17:03.694612	FAILED	FAILED	org.springframework.web.reactive.function.client.WebClientResponseException$NotFound: 404 Not Found from POST http://host.docker.internal:4000/\n\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\tSuppressed: The stacktrace has been enhanced by Reactor, refer to additional information below: \nError has been observed at the following site(s):\n\t*__checkpoint ⇢ 404 NOT_FOUND from POST http://host.docker.internal:4000/ [DefaultWebClient]\nOriginal Stack Trace:\n\t\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\t\tat org.springframework.web.reactive.function.client.DefaultClientResponse.lambda$createException$1(DefaultClientResponse.java:214)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:106)\n\t\tat reactor.core.publisher.FluxOnErrorReturn$ReturnSubscriber.onNext(FluxOnErrorReturn.java:162)\n\t\tat reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:122)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:129)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.onNext(FluxMapFuseable.java:299)\n\t\tat reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.onNext(FluxFilterFuseable.java:337)\n\t\tat reactor.core.publisher.Operators$BaseFluxToMonoOperator.completePossiblyEmpty(Operators.java:2097)\n\t\tat reactor.core.publisher.MonoCollect$CollectSubscriber.onComplete(MonoCollect.java:145)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:415)\n\t\tat reactor.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:446)\n\t\tat reactor.netty.channel.ChannelOperations.terminate(ChannelOperations.java:500)\n\t\tat reactor.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:768)\n\t\tat reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:114)\n\t\tat io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)\n\t\tat 	2025-10-09 02:17:03.694677
14	2	14	2025-10-09 03:06:18.042417	2025-10-09 03:06:18.056518	2025-10-09 03:15:42.381897	STARTED	UNKNOWN		2025-10-09 03:15:42.382083
15	2	15	2025-10-09 03:16:00.892607	2025-10-09 03:16:00.911358	2025-10-09 03:20:20.013521	FAILED	FAILED	org.springframework.web.reactive.function.client.WebClientResponseException$NotFound: 404 Not Found from POST http://host.docker.internal:4000/api/v1/place\n\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\tSuppressed: The stacktrace has been enhanced by Reactor, refer to additional information below: \nError has been observed at the following site(s):\n\t*__checkpoint ⇢ 404 NOT_FOUND from POST http://host.docker.internal:4000/api/v1/place [DefaultWebClient]\nOriginal Stack Trace:\n\t\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\t\tat org.springframework.web.reactive.function.client.DefaultClientResponse.lambda$createException$1(DefaultClientResponse.java:214)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:106)\n\t\tat reactor.core.publisher.FluxOnErrorReturn$ReturnSubscriber.onNext(FluxOnErrorReturn.java:162)\n\t\tat reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:122)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:129)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.onNext(FluxMapFuseable.java:299)\n\t\tat reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.onNext(FluxFilterFuseable.java:337)\n\t\tat reactor.core.publisher.Operators$BaseFluxToMonoOperator.completePossiblyEmpty(Operators.java:2097)\n\t\tat reactor.core.publisher.MonoCollect$CollectSubscriber.onComplete(MonoCollect.java:145)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:415)\n\t\tat reactor.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:446)\n\t\tat reactor.netty.channel.ChannelOperations.terminate(ChannelOperations.java:500)\n\t\tat reactor.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:768)\n\t\tat reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:114)\n\t\tat io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandle	2025-10-09 03:20:20.013579
32	1	32	2025-10-10 04:08:01.28007	2025-10-10 04:08:01.3056	\N	STARTED	UNKNOWN		2025-10-10 04:08:01.306485
22	2	22	2025-10-09 14:28:19.284336	2025-10-09 14:28:19.310878	2025-10-09 14:36:44.704983	STARTED	UNKNOWN		2025-10-09 14:36:44.705013
20	2	20	2025-10-09 14:25:46.473537	2025-10-09 14:25:46.4774	2025-10-09 14:36:44.734148	STOPPED	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 14:36:44.73421
21	2	21	2025-10-09 14:28:04.065274	2025-10-09 14:28:04.070031	2025-10-09 14:36:45.658412	STOPPED	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 14:36:45.658465
23	2	23	2025-10-09 14:31:20.954524	2025-10-09 14:31:20.977639	2025-10-09 14:36:45.911162	STARTED	UNKNOWN		2025-10-09 14:36:45.911186
24	1	24	2025-10-09 14:42:20.14832	2025-10-09 14:42:20.204413	\N	STARTED	UNKNOWN		2025-10-09 14:42:20.204931
28	2	28	2025-10-09 15:06:16.146103	2025-10-09 15:06:16.189844	2025-10-09 15:11:22.212854	UNKNOWN	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 15:11:22.213313
27	2	27	2025-10-09 14:59:07.394959	2025-10-09 14:59:07.543601	2025-10-09 15:11:22.213109	UNKNOWN	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 15:11:22.213307
29	1	29	2025-10-09 15:28:02.425834	2025-10-09 15:28:02.457286	\N	STARTED	UNKNOWN		2025-10-09 15:28:02.458399
30	1	30	2025-10-10 03:34:22.390838	2025-10-10 03:34:22.437481	\N	STARTED	UNKNOWN		2025-10-10 03:34:22.440098
31	1	31	2025-10-10 03:45:36.559432	2025-10-10 03:45:36.610089	\N	STARTED	UNKNOWN		2025-10-10 03:45:36.611698
35	1	35	2025-10-10 12:17:36.843164	2025-10-10 12:17:36.856687	\N	STARTED	UNKNOWN		2025-10-10 12:17:36.857255
33	2	33	2025-10-10 12:15:22.227527	2025-10-10 12:15:22.250253	2025-10-10 12:16:42.129191	COMPLETED	COMPLETED		2025-10-10 12:16:42.12925
34	1	34	2025-10-10 12:17:18.423069	2025-10-10 12:17:18.427978	\N	STARTED	UNKNOWN		2025-10-10 12:17:18.428551
36	1	36	2025-10-10 12:18:39.041004	2025-10-10 12:18:39.044829	\N	STARTED	UNKNOWN		2025-10-10 12:18:39.045521
37	1	37	2025-10-10 12:24:02.566408	2025-10-10 12:24:02.572239	\N	STARTED	UNKNOWN		2025-10-10 12:24:02.573623
75	1	75	2025-10-14 08:50:35.635174	2025-10-14 08:50:35.672564	\N	STARTED	UNKNOWN		2025-10-14 08:50:35.673996
76	1	76	2025-10-14 08:52:56.174339	2025-10-14 08:52:56.223475	\N	STARTED	UNKNOWN		2025-10-14 08:52:56.225912
38	2	38	2025-10-10 12:51:00.35533	2025-10-10 12:51:00.404727	2025-10-10 12:56:41.074146	FAILED	FAILED	org.springframework.retry.ExhaustedRetryException: Retry exhausted after last attempt in recovery path, but exception is not skippable.\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$write$4(FaultTolerantChunkProcessor.java:399)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.write(FaultTolerantChunkProcessor.java:412)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:227)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by	2025-10-10 12:56:41.074232
39	2	39	2025-10-10 13:03:52.10957	2025-10-10 13:03:52.114918	2025-10-10 13:09:58.462578	FAILED	FAILED	org.springframework.retry.ExhaustedRetryException: Retry exhausted after last attempt in recovery path, but exception is not skippable.\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$write$4(FaultTolerantChunkProcessor.java:399)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.write(FaultTolerantChunkProcessor.java:412)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:227)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by	2025-10-10 13:09:58.462728
56	1	56	2025-10-14 03:19:11.890095	2025-10-14 03:19:11.913806	\N	STARTED	UNKNOWN		2025-10-14 03:19:11.914766
40	2	40	2025-10-11 01:55:34.113576	2025-10-11 01:55:34.133241	2025-10-11 01:59:10.829466	FAILED	FAILED	org.springframework.retry.ExhaustedRetryException: Retry exhausted after last attempt in recovery path, but exception is not skippable.\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$write$4(FaultTolerantChunkProcessor.java:399)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.write(FaultTolerantChunkProcessor.java:412)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:227)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by	2025-10-11 01:59:10.829574
41	1	41	2025-10-11 02:09:57.046323	2025-10-11 02:09:57.066024	\N	STARTED	UNKNOWN		2025-10-11 02:09:57.066759
42	1	42	2025-10-11 02:22:02.374356	2025-10-11 02:22:02.389784	\N	STARTED	UNKNOWN		2025-10-11 02:22:02.390245
43	1	43	2025-10-11 13:01:45.945687	2025-10-11 13:01:45.970124	\N	STARTED	UNKNOWN		2025-10-11 13:01:45.971118
44	1	44	2025-10-11 13:01:52.391537	2025-10-11 13:01:52.395349	\N	STARTED	UNKNOWN		2025-10-11 13:01:52.396391
45	1	45	2025-10-11 13:02:23.979321	2025-10-11 13:02:23.994842	\N	STARTED	UNKNOWN		2025-10-11 13:02:23.995403
46	1	46	2025-10-12 00:36:05.647664	2025-10-12 00:36:05.664776	\N	STARTED	UNKNOWN		2025-10-12 00:36:05.665423
47	1	47	2025-10-12 00:46:26.448683	2025-10-12 00:46:26.475616	\N	STARTED	UNKNOWN		2025-10-12 00:46:26.476551
48	1	48	2025-10-12 13:06:07.553191	2025-10-12 13:06:07.565114	\N	STARTED	UNKNOWN		2025-10-12 13:06:07.565659
49	1	49	2025-10-13 00:03:48.242654	2025-10-13 00:03:48.260661	\N	STARTED	UNKNOWN		2025-10-13 00:03:48.260895
50	1	50	2025-10-13 00:41:15.715331	2025-10-13 00:41:15.768809	\N	STARTED	UNKNOWN		2025-10-13 00:41:15.769186
51	1	51	2025-10-14 00:28:04.120086	2025-10-14 00:28:04.137525	\N	STARTED	UNKNOWN		2025-10-14 00:28:04.13838
52	1	52	2025-10-14 02:52:14.632355	2025-10-14 02:52:14.682385	\N	STARTED	UNKNOWN		2025-10-14 02:52:14.684747
53	2	53	2025-10-14 02:54:37.786274	2025-10-14 02:54:37.815124	2025-10-14 02:56:47.391933	FAILED	FAILED	org.springframework.retry.RetryException: Non-skippable exception in recoverer while processing\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$transform$1(FaultTolerantChunkProcessor.java:284)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.transform(FaultTolerantChunkProcessor.java:291)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:220)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by: org.springframework.orm.jpa.Jp	2025-10-14 02:56:47.392152
54	1	54	2025-10-14 02:59:39.16216	2025-10-14 02:59:39.195171	\N	STARTED	UNKNOWN		2025-10-14 02:59:39.196362
57	1	57	2025-10-14 03:26:02.340483	2025-10-14 03:26:02.378294	\N	STARTED	UNKNOWN		2025-10-14 03:26:02.379497
55	2	55	2025-10-14 03:00:11.187368	2025-10-14 03:00:11.216527	2025-10-14 03:01:48.463787	FAILED	FAILED	org.springframework.retry.RetryException: Non-skippable exception in recoverer while processing\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$transform$1(FaultTolerantChunkProcessor.java:284)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.transform(FaultTolerantChunkProcessor.java:291)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:220)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by: org.springframework.orm.jpa.Jp	2025-10-14 03:01:48.463882
58	1	58	2025-10-14 03:32:51.366454	2025-10-14 03:32:51.39247	\N	STARTED	UNKNOWN		2025-10-14 03:32:51.393211
59	1	59	2025-10-14 03:41:20.304489	2025-10-14 03:41:20.333158	\N	STARTED	UNKNOWN		2025-10-14 03:41:20.334469
60	1	60	2025-10-14 03:55:21.862884	2025-10-14 03:55:21.896002	\N	STARTED	UNKNOWN		2025-10-14 03:55:21.896957
61	1	61	2025-10-14 04:47:42.735944	2025-10-14 04:47:42.773654	\N	STARTED	UNKNOWN		2025-10-14 04:47:42.774819
62	1	62	2025-10-14 05:18:55.731621	2025-10-14 05:18:55.778925	\N	STARTED	UNKNOWN		2025-10-14 05:18:55.779508
63	1	63	2025-10-14 05:22:24.83584	2025-10-14 05:22:24.85562	\N	STARTED	UNKNOWN		2025-10-14 05:22:24.856267
64	1	64	2025-10-14 05:24:43.444521	2025-10-14 05:24:43.469937	\N	STARTED	UNKNOWN		2025-10-14 05:24:43.470858
65	1	65	2025-10-14 05:27:16.651287	2025-10-14 05:27:16.657158	\N	STARTED	UNKNOWN		2025-10-14 05:27:16.658143
66	1	66	2025-10-14 05:41:36.19631	2025-10-14 05:41:36.235172	\N	STARTED	UNKNOWN		2025-10-14 05:41:36.235498
67	1	67	2025-10-14 05:47:12.010225	2025-10-14 05:47:12.040144	\N	STARTED	UNKNOWN		2025-10-14 05:47:12.04125
68	1	68	2025-10-14 05:53:46.701116	2025-10-14 05:53:46.724665	\N	STARTED	UNKNOWN		2025-10-14 05:53:46.725428
69	1	69	2025-10-14 06:00:01.315779	2025-10-14 06:00:01.426152	\N	STARTED	UNKNOWN		2025-10-14 06:00:01.427727
70	1	70	2025-10-14 06:06:34.254878	2025-10-14 06:06:34.352397	\N	STARTED	UNKNOWN		2025-10-14 06:06:34.355025
71	1	71	2025-10-14 06:10:45.93084	2025-10-14 06:10:45.962101	\N	STARTED	UNKNOWN		2025-10-14 06:10:45.963169
72	1	72	2025-10-14 06:27:51.627473	2025-10-14 06:27:51.656381	\N	STARTED	UNKNOWN		2025-10-14 06:27:51.657373
73	1	73	2025-10-14 08:09:55.16263	2025-10-14 08:09:55.184771	\N	STARTED	UNKNOWN		2025-10-14 08:09:55.18577
74	1	74	2025-10-14 08:46:14.387981	2025-10-14 08:46:14.422948	\N	STARTED	UNKNOWN		2025-10-14 08:46:14.424415
\.


--
-- Data for Name: batch_job_execution_context; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch_job_execution_context (job_execution_id, short_context, serialized_context) FROM stdin;
1	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
2	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
3	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
4	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
5	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
6	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
7	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
8	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
9	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
10	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
11	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
12	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
13	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
14	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
15	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
16	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
17	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
18	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
19	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
22	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
23	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
20	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
21	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
24	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
25	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
26	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
28	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
27	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
29	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
30	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
31	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
32	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
33	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
34	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
35	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
36	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
37	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
38	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
39	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
40	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
41	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
42	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
43	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
44	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
45	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
46	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
47	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
48	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
49	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
50	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
51	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
52	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
53	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
54	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
55	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4weA==	\N
56	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
57	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
58	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
59	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
60	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
61	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
62	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
63	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
64	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
65	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
66	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
67	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
68	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
69	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
70	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
71	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
72	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
73	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
74	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
75	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
76	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==	\N
\.


--
-- Data for Name: batch_job_execution_params; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch_job_execution_params (job_execution_id, parameter_name, parameter_type, parameter_value, identifying) FROM stdin;
1	region	java.lang.String	jeju	Y
1	startTime	java.lang.Long	1759933935626	Y
2	region	java.lang.String	seoul	Y
2	startTime	java.lang.Long	1759933967011	Y
3	region	java.lang.String	jeju	Y
3	startTime	java.lang.Long	1759934203320	Y
4	region	java.lang.String	jeju	Y
4	startTime	java.lang.Long	1759935066213	Y
5	region	java.lang.String	jeju	Y
5	startTime	java.lang.Long	1759935336555	Y
6	region	java.lang.String	jeju	Y
6	startTime	java.lang.Long	1759943321194	Y
7	region	java.lang.String	jeju	Y
7	startTime	java.lang.Long	1759943623859	Y
8	region	java.lang.String	jeju	Y
8	startTime	java.lang.Long	1759943912406	Y
9	region	java.lang.String	jeju	Y
9	startTime	java.lang.Long	1759944188557	Y
10	region	java.lang.String	jeju	Y
10	startTime	java.lang.Long	1759944328331	Y
11	region	java.lang.String	jeju	Y
11	startTime	java.lang.Long	1759944506448	Y
12	time	java.lang.Long	1759976134629	Y
13	time	java.lang.Long	1759976223455	Y
14	time	java.lang.Long	1759979178032	Y
15	time	java.lang.Long	1759979760878	Y
16	region	java.lang.String	jeju	Y
16	startTime	java.lang.Long	1759980738096	Y
17	time	java.lang.Long	1759981104220	Y
18	time	java.lang.Long	1759982141224	Y
19	startTime	java.lang.Long	1760005590681	Y
20	startTime	java.lang.Long	1760019946459	Y
21	startTime	java.lang.Long	1760020084044	Y
22	region	java.lang.String	seoul	Y
22	startTime	java.lang.Long	1760020099252	Y
23	startTime	java.lang.Long	1760020280926	Y
24	startTime	java.lang.Long	1760020940090	Y
25	region	java.lang.String	seoul	Y
25	startTime	java.lang.Long	1760020946021	Y
26	startTime	java.lang.Long	1760020979053	Y
27	startTime	java.lang.Long	1760021947242	Y
28	startTime	java.lang.Long	1760022376079	Y
29	startTime	java.lang.Long	1760023682387	Y
30	startTime	java.lang.Long	1760067262317	Y
31	startTime	java.lang.Long	1760067936531	Y
32	startTime	java.lang.Long	1760069281254	Y
33	startTime	java.lang.Long	1760098522209	Y
34	startTime	java.lang.Long	1760098638412	Y
35	startTime	java.lang.Long	1760098656832	Y
36	startTime	java.lang.Long	1760098719033	Y
37	startTime	java.lang.Long	1760099042544	Y
38	startTime	java.lang.Long	1760100660309	Y
39	startTime	java.lang.Long	1760101432097	Y
40	startTime	java.lang.Long	1760147734096	Y
41	startTime	java.lang.Long	1760148597031	Y
42	startTime	java.lang.Long	1760149322363	Y
43	startTime	java.lang.Long	1760187705922	Y
44	startTime	java.lang.Long	1760187712384	Y
45	startTime	java.lang.Long	1760187743966	Y
46	startTime	java.lang.Long	1760229365633	Y
47	startTime	java.lang.Long	1760229986428	Y
48	startTime	java.lang.Long	1760274367531	Y
49	startTime	java.lang.Long	1760313828222	Y
50	startTime	java.lang.Long	1760316075663	Y
51	startTime	java.lang.Long	1760401684101	Y
52	startTime	java.lang.Long	1760410334584	Y
53	startTime	java.lang.Long	1760410477756	Y
54	startTime	java.lang.Long	1760410779125	Y
55	startTime	java.lang.Long	1760410811143	Y
56	startTime	java.lang.Long	1760411951857	Y
57	startTime	java.lang.Long	1760412362307	Y
58	startTime	java.lang.Long	1760412771336	Y
59	startTime	java.lang.Long	1760413280277	Y
60	startTime	java.lang.Long	1760414121839	Y
61	startTime	java.lang.Long	1760417262645	Y
62	startTime	java.lang.Long	1760419135625	Y
63	startTime	java.lang.Long	1760419344820	Y
64	startTime	java.lang.Long	1760419483418	Y
65	startTime	java.lang.Long	1760419636642	Y
66	startTime	java.lang.Long	1760420496151	Y
67	startTime	java.lang.Long	1760420831987	Y
68	startTime	java.lang.Long	1760421226675	Y
69	startTime	java.lang.Long	1760421601265	Y
70	startTime	java.lang.Long	1760421994190	Y
71	startTime	java.lang.Long	1760422245900	Y
72	startTime	java.lang.Long	1760423271594	Y
73	startTime	java.lang.Long	1760429395141	Y
74	startTime	java.lang.Long	1760431574358	Y
75	startTime	java.lang.Long	1760431835619	Y
76	startTime	java.lang.Long	1760431976148	Y
\.


--
-- Data for Name: batch_job_instance; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch_job_instance (job_instance_id, version, job_name, job_key) FROM stdin;
1	0	placeCollectionJob	fe681941d423c872a841a1639cc63f70
2	0	placeCollectionJob	4b39ffaf3e12c898f04339ac591da681
3	0	placeCollectionJob	a0dbebd96880f394278191f4adb60354
4	0	placeCollectionJob	f6a3296e6a8c332ed78eb53686ba7893
5	0	placeCollectionJob	d28daf8bee56d7c7cf61b66a2f7c58b0
6	0	placeCollectionJob	bfa4cbee587d71037d31424aa5f6de48
7	0	placeCollectionJob	0b61f562e524ac84ac176d368d71bd6a
8	0	placeCollectionJob	b6d6fb45d13dbce5c89ed07f7fa636f2
9	0	placeCollectionJob	c00a13ec6e412003073833393a9092b4
10	0	placeCollectionJob	0f3a3a6454e93379220886557aca3fe0
11	0	placeCollectionJob	10f3e8037f49046e83214c2d42d5de90
12	0	updateCrawledDataJob	61fda676cf37c2369e0647ecc2a62d4d
13	0	updateCrawledDataJob	2be28095f8f4795df41ed52edbad0758
14	0	updateCrawledDataJob	aa1bd9e65361ad9ea674173bec4b6e1b
15	0	updateCrawledDataJob	2a20a5eaa722567821a42ad80a7cc858
16	0	placeCollectionJob	fb8e55bdcf39f7eee28268520448b758
17	0	updateCrawledDataJob	a80c068f3f93ffa3cf2e471cfb3f6750
18	0	updateCrawledDataJob	18b286e956f7d38db5ef5d0fee11c369
19	0	placeCollectionJob	fcf61731970a72d579ca8750a996aeba
20	0	updateCrawledDataJob	89abbd3080cd224795ebaa7a531cda7e
21	0	updateCrawledDataJob	a0cec0db64753b9769e9586c78779af9
22	0	placeCollectionJob	a2bfbef08e28ca6aa358ff092fbbe8bb
23	0	updateCrawledDataJob	d48d781341b4443a37f7830ca08566ba
24	0	updateCrawledDataJob	f8d34b80a68cdb977a2727f0a9fae3ac
25	0	placeCollectionJob	d755206812be0f807441f6bd863bbea1
26	0	updateCrawledDataJob	6646e2566cc9a5857f6ab5feabada86f
27	0	updateCrawledDataJob	71b9db76cc8a97af2e0c382f45e231d4
28	0	updateCrawledDataJob	47a7493f68c2bb657f1d349f523136a3
29	0	updateCrawledDataJob	88285f4bc86dc3f49a1ec8427815a792
30	0	updateCrawledDataJob	f97dc8177aedc9f9b3a44b4f35e62f58
31	0	updateCrawledDataJob	1ba65bf68248f86c3b09b4728d5de009
32	0	updateCrawledDataJob	ec7ae64c7d59c6296260e158777219c8
33	0	updateCrawledDataJob	4b663fd14d3b983a8f16a69ca8f3eebd
34	0	placeCollectionJob	5ce0b9c755c244ff978c131048aa441c
35	0	updateCrawledDataJob	f36eef7ae2851f878c087fe3a2e2584a
36	0	updateCrawledDataJob	a29737ef080c1372c19f7cfb0df1b10a
37	0	updateCrawledDataJob	c2f62daf7f398da32756fde65b9a1aa2
38	0	updateCrawledDataJob	70ff2fdc832ecb9f5327db8f86e5c2e3
39	0	updateCrawledDataJob	a9180d754cc5296b04593ddd8dd7315c
40	0	updateCrawledDataJob	4b95ec6419e602bfe7a572b2723bd815
41	0	updateCrawledDataJob	41e18d5ed6429ce251891e3b01c41062
42	0	updateCrawledDataJob	031fc3775118d47c27d094dcecb9beb5
43	0	updateCrawledDataJob	fac54d11f8a47eb62ef3050ac99e25e3
44	0	updateCrawledDataJob	cc13a136eb39124e6081d0a866a82d95
45	0	updateCrawledDataJob	a921c762c2fc5b57e6dfb96e2d0acb94
46	0	updateCrawledDataJob	e292cbdee1569788e0014ff19ef84b97
47	0	updateCrawledDataJob	301e3e667e4f325629ecaf199a0aa96a
48	0	updateCrawledDataJob	6d176e0e02676bf314f234b2c723be0e
49	0	updateCrawledDataJob	cd50232f7c27c40dad75d84d9b8bb403
50	0	updateCrawledDataJob	51d19a14c25b15b4b2f0a0a118b15bdd
51	0	updateCrawledDataJob	b18c5fb53f321df248841f569bd183e0
52	0	updateCrawledDataJob	c8bd243a0e0725c01441fd8b74c960dd
53	0	updateCrawledDataJob	cb9165a4ce9c4092d0b45f21dfdedef9
54	0	updateCrawledDataJob	a1638428966a57d8c6cb565eef937230
55	0	updateCrawledDataJob	103937acb74224c1b5499e63b95de811
56	0	updateCrawledDataJob	6a9f6a97d9b6729e4e5c56624d64a598
57	0	updateCrawledDataJob	2f646e7e5075255b48335beb86311b12
58	0	updateCrawledDataJob	3286efef6bf640df6e9a9ff19d090dc5
59	0	updateCrawledDataJob	2cd110473d70e2a9291425448ac1522c
60	0	updateCrawledDataJob	e577c59d5dd475bad930801893a180df
61	0	updateCrawledDataJob	53c45a23ebbd3541ef5fc8db03d436bc
62	0	updateCrawledDataJob	3a0719a576f7b29b77201455c8792f2f
63	0	updateCrawledDataJob	968f84e4096bb3c6c23c8b155c6c0753
64	0	updateCrawledDataJob	9ad3affe0fe307e692bfc3d080cb0e83
65	0	updateCrawledDataJob	3fb2e93e721188459b38ec4b1ac37094
66	0	updateCrawledDataJob	f7692f3f2b706b63e2d8fcfb01548acd
67	0	updateCrawledDataJob	bb40f0085d3b44e93956bd478d0d9cb5
68	0	updateCrawledDataJob	6bffeb0d236104d8e81f8f607d4a3d61
69	0	updateCrawledDataJob	57edac3576b770a4dc52a85d17823be8
70	0	updateCrawledDataJob	4a7bb13d0f6c82b0c9359dea921e7d1c
71	0	updateCrawledDataJob	ab3be704d284eab6b7c7e10546f45cec
72	0	updateCrawledDataJob	8be0a879b1a6372dfca667a981c565b4
73	0	updateCrawledDataJob	a4c7bcdc81cfd9b4f04f6b8aa36d4750
74	0	updateCrawledDataJob	2ba1a9321e855ec8b1695367dff6606a
75	0	updateCrawledDataJob	e302de9437433a1c01e736a9fe5fbfab
76	0	updateCrawledDataJob	8289ecaf6ebf0315ac9f14e0f1b24c3b
\.


--
-- Data for Name: batch_step_execution; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch_step_execution (step_execution_id, version, step_name, job_execution_id, create_time, start_time, end_time, status, commit_count, read_count, filter_count, write_count, read_skip_count, write_skip_count, process_skip_count, rollback_count, exit_code, exit_message, last_updated) FROM stdin;
13	2	updateCrawledDataStep	13	2025-10-09 02:17:03.480983	2025-10-09 02:17:03.482429	2025-10-09 02:17:03.69275	FAILED	0	10	0	0	0	0	0	1	FAILED	org.springframework.web.reactive.function.client.WebClientResponseException$NotFound: 404 Not Found from POST http://host.docker.internal:4000/\n\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\tSuppressed: The stacktrace has been enhanced by Reactor, refer to additional information below: \nError has been observed at the following site(s):\n\t*__checkpoint ⇢ 404 NOT_FOUND from POST http://host.docker.internal:4000/ [DefaultWebClient]\nOriginal Stack Trace:\n\t\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\t\tat org.springframework.web.reactive.function.client.DefaultClientResponse.lambda$createException$1(DefaultClientResponse.java:214)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:106)\n\t\tat reactor.core.publisher.FluxOnErrorReturn$ReturnSubscriber.onNext(FluxOnErrorReturn.java:162)\n\t\tat reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:122)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:129)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.onNext(FluxMapFuseable.java:299)\n\t\tat reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.onNext(FluxFilterFuseable.java:337)\n\t\tat reactor.core.publisher.Operators$BaseFluxToMonoOperator.completePossiblyEmpty(Operators.java:2097)\n\t\tat reactor.core.publisher.MonoCollect$CollectSubscriber.onComplete(MonoCollect.java:145)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:415)\n\t\tat reactor.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:446)\n\t\tat reactor.netty.channel.ChannelOperations.terminate(ChannelOperations.java:500)\n\t\tat reactor.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:768)\n\t\tat reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:114)\n\t\tat io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)\n\t\tat 	2025-10-09 02:17:03.692943
4	27	placeCollectionStep	4	2025-10-08 14:51:06.305604	2025-10-08 14:51:06.308568	2025-10-08 14:51:20.983733	COMPLETED	25	248	239	9	0	0	0	0	COMPLETED		2025-10-08 14:51:20.984773
16	27	placeCollectionStep	16	2025-10-09 03:32:18.111466	2025-10-09 03:32:18.112857	2025-10-09 03:33:33.408901	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-09 03:33:33.409798
14	2	updateCrawledDataStep	14	2025-10-09 03:06:18.060569	2025-10-09 03:06:18.061881	2025-10-09 03:15:42.370677	UNKNOWN	0	10	0	0	0	0	0	1	FAILED	java.lang.NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy\n\tat ch.qos.logback.classic.spi.LoggingEvent.<init>(LoggingEvent.java:143)\n\tat ch.qos.logback.classic.Logger.buildLoggingEventAndAppend(Logger.java:424)\n\tat ch.qos.logback.classic.Logger.filterAndLog_0_Or3Plus(Logger.java:386)\n\tat ch.qos.logback.classic.Logger.log(Logger.java:780)\n\tat org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog.error(LogAdapter.java:431)\n\tat org.springframework.transaction.support.TransactionTemplate.rollbackOnException(TransactionTemplate.java:176)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:144)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:148)\n\tat org.springframework.batch.core.launch.support.TaskExecutorJobLauncher.run(TaskExecutorJobLauncher.java:59)\n\tat com.mohe.spring.controller.BatchController.runUpdateCrawledDataJob(BatchController.java:30)\n\tat java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)\n\tat java.base/java.lang.reflect.Method.invoke(Unknown Source)\n\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:254)\n\tat org.springframework.web.method.support.InvocableHan	2025-10-09 03:15:42.372446
17	5	updateCrawledDataStep	17	2025-10-09 03:38:24.24722	2025-10-09 03:38:24.248465	2025-10-09 03:53:53.641523	STOPPED	3	30	10	20	0	0	0	0	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 03:53:53.642542
3	27	placeCollectionStep	3	2025-10-08 14:36:43.337414	2025-10-08 14:36:43.338696	2025-10-08 14:36:53.625068	COMPLETED	25	248	215	33	0	0	0	0	COMPLETED		2025-10-08 14:36:53.625719
15	2	updateCrawledDataStep	15	2025-10-09 03:16:00.917521	2025-10-09 03:16:00.919244	2025-10-09 03:20:20.010374	FAILED	0	10	0	0	0	0	0	1	FAILED	org.springframework.web.reactive.function.client.WebClientResponseException$NotFound: 404 Not Found from POST http://host.docker.internal:4000/api/v1/place\n\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\tSuppressed: The stacktrace has been enhanced by Reactor, refer to additional information below: \nError has been observed at the following site(s):\n\t*__checkpoint ⇢ 404 NOT_FOUND from POST http://host.docker.internal:4000/api/v1/place [DefaultWebClient]\nOriginal Stack Trace:\n\t\tat org.springframework.web.reactive.function.client.WebClientResponseException.create(WebClientResponseException.java:314)\n\t\tat org.springframework.web.reactive.function.client.DefaultClientResponse.lambda$createException$1(DefaultClientResponse.java:214)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:106)\n\t\tat reactor.core.publisher.FluxOnErrorReturn$ReturnSubscriber.onNext(FluxOnErrorReturn.java:162)\n\t\tat reactor.core.publisher.FluxDefaultIfEmpty$DefaultIfEmptySubscriber.onNext(FluxDefaultIfEmpty.java:122)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:129)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)\n\t\tat reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.onNext(FluxMapFuseable.java:299)\n\t\tat reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.onNext(FluxFilterFuseable.java:337)\n\t\tat reactor.core.publisher.Operators$BaseFluxToMonoOperator.completePossiblyEmpty(Operators.java:2097)\n\t\tat reactor.core.publisher.MonoCollect$CollectSubscriber.onComplete(MonoCollect.java:145)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)\n\t\tat reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)\n\t\tat reactor.netty.channel.FluxReceive.onInboundComplete(FluxReceive.java:415)\n\t\tat reactor.netty.channel.ChannelOperations.onInboundComplete(ChannelOperations.java:446)\n\t\tat reactor.netty.channel.ChannelOperations.terminate(ChannelOperations.java:500)\n\t\tat reactor.netty.http.client.HttpClientOperations.onInboundNext(HttpClientOperations.java:768)\n\t\tat reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:114)\n\t\tat io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandle	2025-10-09 03:20:20.01068
18	9	updateCrawledDataStep	18	2025-10-09 03:55:41.264977	2025-10-09 03:55:41.2672	2025-10-09 04:17:20.227758	STOPPED	7	70	42	28	0	0	0	0	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 04:17:20.228148
19	331	placeCollectionStep	19	2025-10-09 10:26:30.728926	2025-10-09 10:26:30.731526	2025-10-09 12:20:16.373262	COMPLETED	329	3288	3288	0	0	0	0	0	COMPLETED		2025-10-09 12:20:16.377099
2	280	placeCollectionStep	2	2025-10-08 14:32:47.029079	2025-10-08 14:32:47.032165	2025-10-08 14:35:36.187502	COMPLETED	278	2776	1856	920	0	0	0	0	COMPLETED		2025-10-08 14:35:36.187836
36	3	updateCrawledDataStep	36	2025-10-10 12:18:39.054741	2025-10-10 12:18:39.060606	\N	STARTED	2	20	14	6	0	0	0	0	EXECUTING		2025-10-10 12:47:55.981678
9	27	placeCollectionStep	9	2025-10-08 17:23:08.613558	2025-10-08 17:23:08.615432	2025-10-08 17:23:49.235725	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 17:23:49.237372
8	27	placeCollectionStep	8	2025-10-08 17:18:32.459825	2025-10-08 17:18:32.462096	2025-10-08 17:19:10.99258	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 17:19:10.993582
1	27	placeCollectionStep	1	2025-10-08 14:32:15.706555	2025-10-08 14:32:15.708955	2025-10-08 14:32:31.293621	COMPLETED	25	248	126	122	0	0	0	0	COMPLETED		2025-10-08 14:32:31.296129
7	27	placeCollectionStep	7	2025-10-08 17:13:43.894916	2025-10-08 17:13:43.896626	2025-10-08 17:14:23.791871	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 17:14:23.796127
10	27	placeCollectionStep	10	2025-10-08 17:25:28.372501	2025-10-08 17:25:28.374741	2025-10-08 17:26:07.099586	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 17:26:07.100826
6	27	placeCollectionStep	6	2025-10-08 17:08:41.250642	2025-10-08 17:08:41.252229	2025-10-08 17:09:20.507535	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 17:09:20.509461
11	27	placeCollectionStep	11	2025-10-08 17:28:26.492064	2025-10-08 17:28:26.494469	2025-10-08 17:30:28.443016	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 17:30:28.445726
5	27	placeCollectionStep	5	2025-10-08 14:55:36.602173	2025-10-08 14:55:36.603833	2025-10-08 14:56:24.34344	COMPLETED	25	248	248	0	0	0	0	0	COMPLETED		2025-10-08 14:56:24.344672
12	2	updateCrawledDataStep	12	2025-10-09 02:15:34.65905	2025-10-09 02:15:34.660576	2025-10-09 02:15:34.812713	FAILED	0	10	0	0	0	0	0	1	FAILED	org.springframework.web.reactive.function.client.WebClientRequestException: Connection refused: localhost/127.0.0.1:4000\n\tat org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:136)\n\tSuppressed: The stacktrace has been enhanced by Reactor, refer to additional information below: \nError has been observed at the following site(s):\n\t*__checkpoint ⇢ Request to POST http://localhost:4000/ [DefaultWebClient]\nOriginal Stack Trace:\n\t\tat org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:136)\n\t\tat reactor.core.publisher.MonoErrorSupplied.subscribe(MonoErrorSupplied.java:55)\n\t\tat reactor.core.publisher.Mono.subscribe(Mono.java:4512)\n\t\tat reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:103)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222)\n\t\tat reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222)\n\t\tat reactor.core.publisher.MonoNext$NextSubscriber.onError(MonoNext.java:93)\n\t\tat reactor.core.publisher.MonoFlatMapMany$FlatMapManyMain.onError(MonoFlatMapMany.java:204)\n\t\tat reactor.core.publisher.SerializedSubscriber.onError(SerializedSubscriber.java:124)\n\t\tat reactor.core.publisher.FluxRetryWhen$RetryWhenMainSubscriber.whenError(FluxRetryWhen.java:228)\n\t\tat reactor.core.publisher.FluxRetryWhen$RetryWhenOtherSubscriber.onError(FluxRetryWhen.java:278)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onError(FluxContextWrite.java:121)\n\t\tat reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.maybeOnError(FluxConcatMapNoPrefetch.java:326)\n\t\tat reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.onNext(FluxConcatMapNoPrefetch.java:211)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)\n\t\tat reactor.core.publisher.SinkManyEmitterProcessor.drain(SinkManyEmitterProcessor.java:476)\n\t\tat reactor.core.publisher.SinkManyEmitterProcessor$EmitterInner.drainParent(SinkManyEmitterProcessor.java:620)\n\t\tat reactor.core.publisher.FluxPublish$PubSubInner.request(FluxPublish.java:874)\n\t\tat reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.request(FluxContextWrite.java:136)\n\t\tat reactor.core.publisher.FluxConcatMapNoPref	2025-10-09 02:15:34.812905
27	10	updateCrawledDataStep	27	2025-10-09 14:59:07.566225	2025-10-09 14:59:07.569558	2025-10-09 15:11:22.188496	UNKNOWN	8	90	82	6	0	0	0	1	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 15:11:22.190781
28	2	updateCrawledDataStep	28	2025-10-09 15:06:16.20806	2025-10-09 15:06:16.215991	2025-10-09 15:11:22.188934	UNKNOWN	0	10	9	0	0	0	0	1	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 15:11:22.191083
39	2	updateCrawledDataStep	39	2025-10-10 13:03:52.12044	2025-10-10 13:03:52.122758	2025-10-10 13:09:58.457409	FAILED	0	10	6	0	0	0	0	2	FAILED	org.springframework.retry.ExhaustedRetryException: Retry exhausted after last attempt in recovery path, but exception is not skippable.\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$write$4(FaultTolerantChunkProcessor.java:399)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.write(FaultTolerantChunkProcessor.java:412)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:227)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by	2025-10-10 13:09:58.457929
37	2	updateCrawledDataStep	37	2025-10-10 12:24:02.607347	2025-10-10 12:24:02.617952	\N	STARTED	1	10	8	1	0	0	0	2	EXECUTING		2025-10-10 12:49:34.613586
54	1	updateCrawledDataStep	54	2025-10-14 02:59:39.208602	2025-10-14 02:59:39.212463	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 02:59:39.213701
59	1	updateCrawledDataStep	59	2025-10-14 03:41:20.347754	2025-10-14 03:41:20.374197	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 03:41:20.376129
63	1	updateCrawledDataStep	63	2025-10-14 05:22:24.865162	2025-10-14 05:22:24.867514	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 05:22:24.868292
66	1	updateCrawledDataStep	66	2025-10-14 05:41:36.246324	2025-10-14 05:41:36.249213	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 05:41:36.249961
23	1	updateCrawledDataStep	23	2025-10-09 14:31:20.989871	2025-10-09 14:31:20.991804	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-09 14:31:20.99187
69	1	updateCrawledDataStep	69	2025-10-14 06:00:01.47041	2025-10-14 06:00:01.479533	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 06:00:01.481624
72	1	updateCrawledDataStep	72	2025-10-14 06:27:51.670473	2025-10-14 06:27:51.67346	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 06:27:51.67416
75	1	updateCrawledDataStep	75	2025-10-14 08:50:35.722789	2025-10-14 08:50:35.735044	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 08:50:35.741351
43	1	updateCrawledDataStep	43	2025-10-11 13:01:46.035421	2025-10-11 13:01:46.039599	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-11 13:01:46.042389
40	2	updateCrawledDataStep	40	2025-10-11 01:55:34.143341	2025-10-11 01:55:34.146508	2025-10-11 01:59:10.82366	FAILED	0	10	6	0	0	0	0	2	FAILED	org.springframework.retry.ExhaustedRetryException: Retry exhausted after last attempt in recovery path, but exception is not skippable.\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$write$4(FaultTolerantChunkProcessor.java:399)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.write(FaultTolerantChunkProcessor.java:412)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:227)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by	2025-10-11 01:59:10.824258
47	33	updateCrawledDataStep	47	2025-10-12 00:46:26.488585	2025-10-12 00:46:26.5071	\N	STARTED	32	320	181	139	0	0	0	0	EXECUTING		2025-10-12 16:28:33.550729
41	3	updateCrawledDataStep	41	2025-10-11 02:09:57.088468	2025-10-11 02:09:57.092955	\N	STARTED	2	20	7	13	0	0	0	0	EXECUTING		2025-10-11 02:16:50.795745
20	4	updateCrawledDataStep	20	2025-10-09 14:25:46.480092	2025-10-09 14:25:46.481167	2025-10-09 14:36:44.686288	STOPPED	2	20	17	3	0	0	0	0	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 14:36:44.687077
22	120	placeCollectionStep	22	2025-10-09 14:28:19.331477	2025-10-09 14:28:19.343944	2025-10-09 14:36:44.701489	UNKNOWN	118	1190	1180	0	0	0	0	1	FAILED	java.lang.NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy\n\tat ch.qos.logback.classic.spi.LoggingEvent.<init>(LoggingEvent.java:143)\n\tat ch.qos.logback.classic.Logger.buildLoggingEventAndAppend(Logger.java:424)\n\tat ch.qos.logback.classic.Logger.filterAndLog_0_Or3Plus(Logger.java:386)\n\tat ch.qos.logback.classic.Logger.log(Logger.java:780)\n\tat org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog.error(LogAdapter.java:431)\n\tat org.springframework.transaction.support.TransactionTemplate.rollbackOnException(TransactionTemplate.java:176)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:144)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:148)\n\tat org.springframework.batch.core.launch.support.TaskExecutorJobLauncher.run(TaskExecutorJobLauncher.java:59)\n\tat com.mohe.spring.controller.BatchJobController.runPlaceCollectionJobWithRegion(BatchJobController.java:64)\n\tat java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)\n\tat java.base/java.lang.reflect.Method.invoke(Unknown Source)\n\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:254)\n\tat org.springframework.web.method.suppor	2025-10-09 14:36:44.701704
21	3	updateCrawledDataStep	21	2025-10-09 14:28:04.078372	2025-10-09 14:28:04.08296	2025-10-09 14:36:45.644693	STOPPED	1	10	8	2	0	0	0	0	STOPPED	org.springframework.batch.core.JobInterruptedException	2025-10-09 14:36:45.644828
46	2	updateCrawledDataStep	46	2025-10-12 00:36:05.67477	2025-10-12 00:36:05.688871	\N	STARTED	1	10	10	0	0	0	0	0	EXECUTING		2025-10-12 00:39:45.739872
55	2	updateCrawledDataStep	55	2025-10-14 03:00:11.230234	2025-10-14 03:00:11.232927	2025-10-14 03:01:48.460216	FAILED	0	10	0	0	0	0	0	2	FAILED	org.springframework.retry.RetryException: Non-skippable exception in recoverer while processing\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$transform$1(FaultTolerantChunkProcessor.java:284)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.transform(FaultTolerantChunkProcessor.java:291)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:220)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by: org.springframework.orm.jpa.Jp	2025-10-14 03:01:48.460651
29	13	updateCrawledDataStep	29	2025-10-09 15:28:02.506342	2025-10-09 15:28:02.511627	\N	STARTED	12	120	101	19	0	0	0	0	EXECUTING		2025-10-09 15:46:17.207892
30	1	updateCrawledDataStep	30	2025-10-10 03:34:22.476442	2025-10-10 03:34:22.483097	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-10 03:34:22.484358
49	7	updateCrawledDataStep	49	2025-10-13 00:03:48.269388	2025-10-13 00:03:48.272168	\N	STARTED	6	60	43	17	0	0	0	0	EXECUTING		2025-10-13 00:32:35.108796
42	34	updateCrawledDataStep	42	2025-10-11 02:22:02.407612	2025-10-11 02:22:02.411193	\N	STARTED	33	330	277	53	0	0	0	0	EXECUTING		2025-10-11 12:52:55.28893
31	3	updateCrawledDataStep	31	2025-10-10 03:45:36.631364	2025-10-10 03:45:36.635117	\N	STARTED	2	20	14	6	0	0	0	0	EXECUTING		2025-10-10 03:54:28.10996
32	3	updateCrawledDataStep	32	2025-10-10 04:08:01.320665	2025-10-10 04:08:01.32469	\N	STARTED	2	20	19	1	0	0	0	0	EXECUTING		2025-10-10 04:19:28.559455
33	3628	updateCrawledDataStep	33	2025-10-10 12:15:22.275069	2025-10-10 12:15:22.285542	2025-10-10 12:16:42.125221	COMPLETED	3626	36258	36258	0	0	0	0	0	COMPLETED		2025-10-10 12:16:42.1261
24	14	updateCrawledDataStep	24	2025-10-09 14:42:20.215604	2025-10-09 14:42:20.219904	\N	STARTED	13	140	130	0	0	0	0	1	EXECUTING		2025-10-09 14:53:28.48121
26	13	updateCrawledDataStep	26	2025-10-09 14:42:59.093557	2025-10-09 14:42:59.098292	\N	STARTED	12	120	118	2	0	0	0	0	EXECUTING		2025-10-09 14:53:28.686706
51	6	updateCrawledDataStep	51	2025-10-14 00:28:04.146994	2025-10-14 00:28:04.149962	\N	STARTED	5	50	41	9	0	0	0	0	EXECUTING		2025-10-14 00:59:32.472495
38	2	updateCrawledDataStep	38	2025-10-10 12:51:00.417513	2025-10-10 12:51:00.420902	2025-10-10 12:56:41.069406	FAILED	0	10	6	0	0	0	0	2	FAILED	org.springframework.retry.ExhaustedRetryException: Retry exhausted after last attempt in recovery path, but exception is not skippable.\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$write$4(FaultTolerantChunkProcessor.java:399)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.write(FaultTolerantChunkProcessor.java:412)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:227)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by	2025-10-10 12:56:41.069974
56	1	updateCrawledDataStep	56	2025-10-14 03:19:11.935576	2025-10-14 03:19:11.948524	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 03:19:11.949715
60	1	updateCrawledDataStep	60	2025-10-14 03:55:21.9126	2025-10-14 03:55:21.91792	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 03:55:21.920531
44	1	updateCrawledDataStep	44	2025-10-11 13:01:52.409314	2025-10-11 13:01:52.415463	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-11 13:01:52.415634
34	4	placeCollectionStep	34	2025-10-10 12:17:18.43235	2025-10-10 12:17:18.433677	\N	STARTED	3	30	30	0	0	0	0	0	EXECUTING		2025-10-10 12:17:29.115574
64	7	updateCrawledDataStep	64	2025-10-14 05:24:43.503024	2025-10-14 05:24:43.508862	\N	STARTED	6	70	60	0	0	0	0	1	EXECUTING		2025-10-14 05:37:19.084456
67	1	updateCrawledDataStep	67	2025-10-14 05:47:12.07643	2025-10-14 05:47:12.084903	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 05:47:12.101026
70	1	updateCrawledDataStep	70	2025-10-14 06:06:34.395264	2025-10-14 06:06:34.39905	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 06:06:34.400557
73	1	updateCrawledDataStep	73	2025-10-14 08:09:55.200667	2025-10-14 08:09:55.204293	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 08:09:55.205237
48	18	updateCrawledDataStep	48	2025-10-12 13:06:07.572561	2025-10-12 13:06:07.578884	\N	STARTED	17	170	77	93	0	0	0	0	EXECUTING		2025-10-12 16:28:09.776129
50	3	updateCrawledDataStep	50	2025-10-13 00:41:15.77994	2025-10-13 00:41:15.782593	\N	STARTED	2	20	20	0	0	0	0	0	EXECUTING		2025-10-13 00:47:59.18939
52	2	updateCrawledDataStep	52	2025-10-14 02:52:14.716174	2025-10-14 02:52:14.722613	\N	STARTED	1	10	10	0	0	0	0	0	EXECUTING		2025-10-14 02:53:59.291736
57	1	updateCrawledDataStep	57	2025-10-14 03:26:02.448319	2025-10-14 03:26:02.457525	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 03:26:02.460311
61	1	updateCrawledDataStep	61	2025-10-14 04:47:42.792256	2025-10-14 04:47:42.79691	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 04:47:42.797985
76	16	updateCrawledDataStep	76	2025-10-14 08:52:56.248637	2025-10-14 08:52:56.255475	\N	STARTED	15	150	9	141	0	0	0	0	EXECUTING		2025-10-14 10:51:01.2443
53	2	updateCrawledDataStep	53	2025-10-14 02:54:37.829752	2025-10-14 02:54:37.832638	2025-10-14 02:56:47.378656	FAILED	0	10	0	0	0	0	0	2	FAILED	org.springframework.retry.RetryException: Non-skippable exception in recoverer while processing\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.lambda$transform$1(FaultTolerantChunkProcessor.java:284)\n\tat org.springframework.retry.support.RetryTemplate.handleRetryExhausted(RetryTemplate.java:543)\n\tat org.springframework.retry.support.RetryTemplate.doExecute(RetryTemplate.java:389)\n\tat org.springframework.retry.support.RetryTemplate.execute(RetryTemplate.java:255)\n\tat org.springframework.batch.core.step.item.BatchRetryTemplate.execute(BatchRetryTemplate.java:216)\n\tat org.springframework.batch.core.step.item.FaultTolerantChunkProcessor.transform(FaultTolerantChunkProcessor.java:291)\n\tat org.springframework.batch.core.step.item.SimpleChunkProcessor.process(SimpleChunkProcessor.java:220)\n\tat org.springframework.batch.core.step.item.ChunkOrientedTasklet.execute(ChunkOrientedTasklet.java:75)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312)\n\tat org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255)\n\tat org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206)\n\tat org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140)\n\tat org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240)\n\tat org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229)\n\tat org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153)\n\tat org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418)\n\tat org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132)\n\tat org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317)\n\tat org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157)\n\tat java.base/java.lang.Thread.run(Unknown Source)\nCaused by: org.springframework.orm.jpa.Jp	2025-10-14 02:56:47.379394
58	1	updateCrawledDataStep	58	2025-10-14 03:32:51.418191	2025-10-14 03:32:51.423976	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 03:32:51.425219
25	189	placeCollectionStep	25	2025-10-09 14:42:26.041682	2025-10-09 14:42:26.043628	\N	STARTED	188	1880	1880	0	0	0	0	0	EXECUTING		2025-10-09 14:53:27.252725
62	1	updateCrawledDataStep	62	2025-10-14 05:18:55.789619	2025-10-14 05:18:55.793443	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 05:18:55.794926
35	3	updateCrawledDataStep	35	2025-10-10 12:17:36.863436	2025-10-10 12:17:36.86527	\N	STARTED	2	20	13	7	0	0	0	0	EXECUTING		2025-10-10 12:43:51.100201
65	6	updateCrawledDataStep	65	2025-10-14 05:27:16.669547	2025-10-14 05:27:16.673505	\N	STARTED	5	50	50	0	0	0	0	0	EXECUTING		2025-10-14 05:36:18.102185
68	1	updateCrawledDataStep	68	2025-10-14 05:53:46.766226	2025-10-14 05:53:46.77295	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 05:53:46.775673
71	1	updateCrawledDataStep	71	2025-10-14 06:10:45.976564	2025-10-14 06:10:45.98097	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 06:10:45.982569
74	1	updateCrawledDataStep	74	2025-10-14 08:46:14.464318	2025-10-14 08:46:14.478997	\N	STARTED	0	0	0	0	0	0	0	0	EXECUTING		2025-10-14 08:46:14.482469
45	33	updateCrawledDataStep	45	2025-10-11 13:02:24.002335	2025-10-11 13:02:24.004185	\N	STARTED	32	320	105	215	0	0	0	0	EXECUTING		2025-10-11 15:40:11.195576
\.


--
-- Data for Name: batch_step_execution_context; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch_step_execution_context (step_execution_id, short_context, serialized_context) FROM stdin;
12	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
5	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
4	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
6	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
1	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
3	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
14	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
10	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
7	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
2	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
9	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
13	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
8	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
11	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
16	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
15	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
17	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAedAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
20	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAodAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
21	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
18	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAABGdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
19	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
23	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
75	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
25	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
37	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAFdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAUdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVwdAA8b3JnLnNwcmluZ2ZyYW1ld29yay5iYXRjaC5jb3JlLnN0ZXAuaXRlbS5DaHVua01vbml0b3IuT0ZGU0VUc3EAfgADAAAAAXg=	\N
48	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAHqdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
50	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAUdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
54	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
59	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
63	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
67	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
71	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
31	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAUdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
24	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAEDdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
26	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAENdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
27	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAABQdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
22	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
38	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
32	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAUdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
29	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAB4dAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
30	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
40	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
33	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAI2jdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
39	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
41	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAUdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
42	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAFKdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
55	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
28	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAABadAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
56	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
34	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
43	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
60	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
64	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAACCdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
51	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAydAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
68	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
72	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
76	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAACWdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
65	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAB4dAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
35	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAydAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
44	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAKdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
52	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAKdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
57	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
61	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
69	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
73	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
58	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
36	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAA8dAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
62	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
45	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAFAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
66	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
46	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAKdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
49	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAA8dAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
47	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAH0dAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
53	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
70	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
74	rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAAfUmVwb3NpdG9yeUl0ZW1SZWFkZXIucmVhZC5jb3VudHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMS4wdAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVweA==	\N
\.


--
-- Data for Name: bookmarks; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bookmarks (id, user_id, place_id, created_at) FROM stdin;
\.


--
-- Data for Name: email_verifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.email_verifications (id, code, email, issued_at, success, verified_at, user_id) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	initial schema	SQL	V1__initial_schema.sql	1749978194	mohe_user	2025-10-08 14:31:57.762961	81	t
2	2	create spring batch tables	SQL	V2__create_spring_batch_tables.sql	1849417903	mohe_user	2025-10-08 14:31:57.859568	20	t
3	3	add crawler found column	SQL	V3__add_crawler_found_column.sql	2132337702	mohe_user	2025-10-09 03:55:23.725575	8	t
4	4	create place reviews table	SQL	V4__create_place_reviews_table.sql	-108431838	mohe_user	2025-10-14 03:07:44.707984	20	t
\.


--
-- Data for Name: keyword_catalog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.keyword_catalog (id, category, created_at, keyword, mbti_weights, updated_at) FROM stdin;
\.


--
-- Data for Name: password_reset_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.password_reset_tokens (id, user_id, token, expiry_date, created_at, expires_at, used) FROM stdin;
\.


--
-- Data for Name: place_business_hours; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_business_hours (id, place_id, day_of_week, open, close, description, is_operating, last_order_minutes) FROM stdin;
1737	425	금	09:00:00	22:00:00	\N	f	30
1738	425	목	09:00:00	22:00:00	\N	f	30
1739	425	수	09:00:00	22:00:00	\N	f	30
1740	425	월	09:00:00	22:00:00	\N	f	30
1741	425	일	09:00:00	22:00:00	\N	f	30
1742	425	토	09:00:00	22:00:00	\N	f	30
1743	425	화	09:00:00	22:00:00	\N	f	30
1744	426	금	11:00:00	20:00:00	\N	f	\N
1745	426	목	11:00:00	20:00:00	\N	f	\N
1746	426	수	11:00:00	20:00:00	\N	f	\N
1747	426	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1748	426	일	11:00:00	20:00:00	\N	f	\N
1749	426	토	11:00:00	20:00:00	\N	f	\N
1750	426	화	11:00:00	20:00:00	\N	f	\N
1751	427	금	11:00:00	20:00:00	\N	f	\N
1752	427	목	11:00:00	20:00:00	\N	f	\N
1753	427	수	11:00:00	20:00:00	\N	f	\N
1754	427	월	11:00:00	20:00:00	\N	f	\N
1755	427	일	11:00:00	20:00:00	\N	f	\N
1756	427	토	11:00:00	20:00:00	\N	f	\N
1757	427	화	11:00:00	20:00:00	\N	f	\N
2067	534	목	\N	\N	\N	f	\N
85	11	금	11:00:00	21:00:00	\N	f	\N
86	11	목	11:00:00	21:00:00	\N	f	\N
87	11	수	11:00:00	21:00:00	\N	f	\N
88	11	월	11:00:00	20:00:00	\N	f	\N
89	11	일	11:00:00	20:00:00	\N	f	\N
90	11	토	11:00:00	21:00:00	\N	f	\N
91	11	화	11:00:00	21:00:00	\N	f	\N
92	12	금	12:00:00	21:00:00	\N	f	\N
93	12	목	12:00:00	21:00:00	\N	f	\N
94	12	수	12:00:00	21:00:00	\N	f	\N
95	12	월	12:00:00	21:00:00	\N	f	\N
96	12	일	12:00:00	21:00:00	\N	f	\N
97	12	토	12:00:00	21:00:00	\N	f	\N
98	12	화	12:00:00	21:00:00	\N	f	\N
99	13	금	12:00:00	22:00:00	\N	f	30
100	13	목	12:00:00	22:00:00	\N	f	30
101	13	수	12:00:00	22:00:00	\N	f	30
102	13	월	12:00:00	22:00:00	\N	f	30
103	13	일	12:00:00	22:00:00	\N	f	30
104	13	토	12:00:00	22:00:00	\N	f	30
105	13	화	12:00:00	22:00:00	\N	f	30
141	15	금	12:00:00	22:00:00	\N	f	30
142	15	목	12:00:00	22:00:00	\N	f	30
143	15	수	12:00:00	22:00:00	\N	f	30
144	15	월	12:00:00	17:00:00	\N	f	30
145	15	일	12:00:00	22:00:00	\N	f	30
146	15	토	12:00:00	22:00:00	\N	f	30
147	15	화	12:00:00	22:00:00	\N	f	30
148	16	금	11:00:00	18:00:00	\N	f	\N
149	16	목	11:00:00	18:00:00	\N	f	\N
150	16	수	11:00:00	18:00:00	\N	f	\N
151	16	월	11:00:00	18:00:00	\N	f	\N
152	16	일	11:00:00	20:00:00	\N	f	\N
153	16	토	11:00:00	20:00:00	\N	f	\N
154	16	화	11:00:00	18:00:00	\N	f	\N
155	17	금	11:00:00	21:00:00	\N	f	30
156	17	목	11:00:00	21:00:00	\N	f	30
157	17	수	11:00:00	21:00:00	\N	f	30
158	17	월	11:00:00	21:00:00	\N	f	30
159	17	일	11:00:00	21:00:00	\N	f	30
160	17	토	11:00:00	21:00:00	\N	f	30
161	17	화	11:00:00	21:00:00	\N	f	30
176	30	금	09:00:00	18:30:00	\N	f	\N
177	30	목	09:00:00	18:30:00	\N	f	\N
178	30	수	09:00:00	18:30:00	\N	f	\N
179	30	월	09:00:00	18:30:00	\N	f	\N
180	30	일	09:00:00	18:30:00	\N	f	\N
181	30	토	09:00:00	18:30:00	\N	f	\N
182	30	화	09:00:00	18:30:00	\N	f	\N
183	31	금	10:00:00	22:00:00	\N	f	15
184	31	목	10:00:00	14:00:00	\N	f	15
185	31	수	10:00:00	22:00:00	\N	f	15
186	31	월	10:00:00	22:00:00	\N	f	15
187	31	일	10:00:00	22:00:00	\N	f	15
188	31	토	10:00:00	22:00:00	\N	f	15
189	31	화	10:00:00	22:00:00	\N	f	15
190	32	금	10:00:00	18:00:00	\N	f	30
191	32	목	10:00:00	18:00:00	\N	f	30
192	32	수	10:00:00	18:00:00	\N	f	30
193	32	월	10:00:00	18:00:00	\N	f	30
194	32	일	08:00:00	20:00:00	\N	f	30
195	32	토	08:00:00	20:00:00	\N	f	30
196	32	화	10:00:00	18:00:00	\N	f	30
197	33	금	11:00:00	22:00:00	\N	f	10
198	33	목	11:00:00	22:00:00	\N	f	10
199	33	수	11:00:00	22:00:00	\N	f	10
200	33	월	\N	\N	정기휴무 (매주 월요일)	f	10
201	33	일	11:00:00	22:00:00	\N	f	10
202	33	토	11:00:00	22:00:00	\N	f	10
203	33	화	11:00:00	22:00:00	\N	f	10
204	34	금	07:30:00	18:30:00	\N	f	\N
205	34	목	07:30:00	18:30:00	\N	f	\N
206	34	수	07:30:00	18:30:00	\N	f	\N
207	34	월	07:30:00	18:30:00	\N	f	\N
208	34	일	07:30:00	20:00:00	\N	f	\N
209	34	토	07:30:00	20:00:00	\N	f	\N
210	34	화	07:30:00	18:30:00	\N	f	\N
211	35	금	08:00:00	18:00:00	\N	f	\N
212	35	목	08:00:00	18:00:00	\N	f	\N
213	35	수	08:00:00	18:00:00	\N	f	\N
214	35	월	08:00:00	18:00:00	\N	f	\N
215	35	일	08:00:00	18:00:00	\N	f	\N
216	35	토	08:00:00	18:00:00	\N	f	\N
217	35	화	08:00:00	18:00:00	\N	f	\N
246	19	금	12:00:00	18:30:00	\N	f	\N
247	19	목	12:00:00	18:30:00	\N	f	\N
248	19	수	12:00:00	18:30:00	\N	f	\N
249	19	월	12:00:00	18:30:00	\N	f	\N
250	19	일	12:00:00	19:00:00	\N	f	\N
251	19	토	12:00:00	21:00:00	\N	f	\N
252	19	화	12:00:00	18:30:00	\N	f	\N
260	36	금	10:00:00	19:00:00	\N	f	\N
261	36	목	08:00:00	20:00:00	\N	f	\N
262	36	수	08:00:00	20:00:00	\N	f	\N
263	36	월	08:00:00	20:00:00	\N	f	\N
264	36	일	10:00:00	22:00:00	\N	f	\N
265	36	토	10:00:00	22:00:00	\N	f	\N
266	36	화	08:00:00	20:00:00	\N	f	\N
267	38	금	\N	\N	휴무	f	\N
268	38	목	10:00:00	19:00:00	\N	f	\N
269	38	수	10:00:00	19:00:00	\N	f	\N
270	38	월	10:00:00	19:00:00	\N	f	\N
271	38	일	\N	\N	휴무	f	\N
272	38	토	\N	\N	휴무	f	\N
273	38	화	10:00:00	19:00:00	\N	f	\N
288	52	금	12:00:00	21:30:00	\N	f	400
289	52	목	12:00:00	21:30:00	\N	f	400
290	52	수	12:00:00	21:30:00	\N	f	400
291	52	월	12:00:00	21:30:00	\N	f	400
292	52	일	\N	\N	정기휴무 (매주 일요일)	f	400
293	52	토	12:00:00	21:00:00	\N	f	400
294	52	화	12:00:00	21:30:00	\N	f	400
1758	428	금	11:30:00	21:00:00	\N	f	\N
1759	428	목	11:30:00	21:00:00	\N	f	\N
1760	428	수	11:30:00	21:00:00	\N	f	\N
1761	428	월	11:30:00	21:00:00	\N	f	\N
1762	428	일	11:30:00	20:30:00	\N	f	\N
1763	428	토	11:30:00	20:30:00	\N	f	\N
1764	428	화	11:30:00	21:00:00	\N	f	\N
1765	429	금	11:00:00	21:30:00	\N	f	60
1766	429	목	11:00:00	21:30:00	\N	f	60
1767	429	수	11:00:00	21:30:00	\N	f	60
1768	429	월	11:00:00	21:30:00	\N	f	60
316	18	금	\N	\N	\N	f	\N
317	18	목	\N	\N	\N	f	\N
318	18	수	\N	\N	\N	f	\N
319	18	월	\N	\N	\N	f	\N
320	18	일	\N	\N	\N	f	\N
321	18	토	\N	\N	\N	f	\N
322	18	화	\N	\N	\N	f	\N
365	71	금	11:30:00	15:30:00	\N	f	60
366	71	목	11:30:00	15:30:00	\N	f	60
367	71	수	11:30:00	15:30:00	\N	f	60
368	71	월	11:30:00	15:30:00	\N	f	60
369	71	일	\N	\N	정기휴무 (매주 일요일)	f	60
370	71	토	11:30:00	21:00:00	\N	f	60
371	71	화	11:30:00	15:30:00	\N	f	60
372	74	금	11:30:00	17:00:00	\N	f	30
373	74	목	11:30:00	17:00:00	\N	f	30
374	74	수	11:30:00	17:00:00	\N	f	30
375	74	월	11:30:00	17:00:00	\N	f	30
376	74	일	\N	\N	정기휴무 (매주 일요일)	f	30
377	74	토	12:00:00	19:00:00	\N	f	30
378	74	화	11:30:00	17:00:00	\N	f	30
379	78	금	12:00:00	22:00:00	\N	f	420
380	78	목	12:00:00	22:00:00	\N	f	420
381	78	수	12:00:00	22:00:00	\N	f	420
382	78	월	\N	\N	정기휴무 (매주 월요일)	f	420
383	78	일	12:00:00	22:00:00	\N	f	420
384	78	토	12:00:00	22:00:00	\N	f	420
385	78	화	12:00:00	22:00:00	\N	f	420
1769	429	일	11:00:00	21:30:00	\N	f	60
1770	429	토	11:00:00	21:30:00	\N	f	60
1771	429	화	11:00:00	21:30:00	\N	f	60
1772	430	금	10:30:00	21:00:00	\N	f	30
1773	430	목	10:30:00	21:00:00	\N	f	30
1774	430	수	10:30:00	21:00:00	\N	f	30
1775	430	월	10:30:00	21:00:00	\N	f	30
1776	430	일	10:30:00	21:00:00	\N	f	30
1777	430	토	10:30:00	21:00:00	\N	f	30
1778	430	화	10:30:00	21:00:00	\N	f	30
1779	431	금	11:00:00	21:30:00	\N	f	390
1780	431	목	11:00:00	21:30:00	\N	f	390
1781	431	수	11:00:00	21:30:00	\N	f	390
1782	431	월	11:00:00	21:30:00	\N	f	390
1783	431	일	11:00:00	21:30:00	\N	f	390
1784	431	토	11:00:00	21:30:00	\N	f	390
1785	431	화	11:00:00	21:30:00	\N	f	390
1786	432	금	11:30:00	21:00:00	\N	f	360
1787	432	목	11:30:00	21:00:00	\N	f	360
1788	432	수	11:30:00	21:00:00	\N	f	360
1789	432	월	11:30:00	21:00:00	\N	f	360
1790	432	일	11:30:00	21:00:00	\N	f	360
1791	432	토	11:30:00	21:00:00	\N	f	360
1792	432	화	11:30:00	21:00:00	\N	f	360
1793	433	금	07:30:00	20:00:00	\N	f	\N
1794	433	목	07:30:00	20:00:00	\N	f	\N
1795	433	수	07:30:00	20:00:00	\N	f	\N
1796	433	월	07:30:00	20:00:00	\N	f	\N
1797	433	일	07:30:00	20:00:00	\N	f	\N
1798	433	토	07:30:00	20:00:00	\N	f	\N
1799	433	화	07:30:00	20:00:00	\N	f	\N
1800	434	금	11:00:00	19:00:00	\N	f	\N
1801	434	목	11:00:00	19:00:00	\N	f	\N
1802	434	수	11:00:00	19:00:00	\N	f	\N
1803	434	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1804	434	일	11:00:00	18:00:00	\N	f	\N
1805	434	토	11:00:00	19:00:00	\N	f	\N
1806	434	화	11:00:00	19:00:00	\N	f	\N
1858	456	수	12:00:00	22:00:00	\N	f	\N
1859	456	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1860	456	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1861	456	토	12:00:00	22:00:00	\N	f	\N
1862	456	화	12:00:00	22:00:00	\N	f	\N
1863	465	금	11:30:00	22:30:00	\N	f	450
1864	465	목	11:30:00	22:00:00	\N	f	450
1865	465	수	11:30:00	22:00:00	\N	f	450
1866	465	월	\N	\N	정기휴무 (매주 월요일)	f	450
1867	465	일	11:30:00	22:00:00	\N	f	450
1868	465	토	11:30:00	22:30:00	\N	f	450
1869	465	화	11:30:00	22:00:00	\N	f	450
1870	466	금	11:00:00	20:00:00	\N	f	\N
1871	466	목	11:00:00	20:00:00	\N	f	\N
1872	466	수	11:00:00	20:00:00	\N	f	\N
1873	466	월	11:00:00	20:00:00	\N	f	\N
1874	466	일	11:00:00	20:00:00	\N	f	\N
1875	466	토	11:00:00	20:00:00	\N	f	\N
1876	466	화	11:00:00	20:00:00	\N	f	\N
1877	469	금	11:30:00	21:30:00	\N	f	390
1878	469	목	11:30:00	21:30:00	\N	f	390
1879	469	수	11:30:00	21:30:00	\N	f	390
1880	469	월	\N	\N	정기휴무 (매주 월요일)	f	390
1881	469	일	11:30:00	21:30:00	\N	f	390
1882	469	토	11:30:00	21:30:00	\N	f	390
1883	469	화	11:30:00	21:30:00	\N	f	390
1884	470	금	11:30:00	21:30:00	\N	f	390
1885	470	목	11:30:00	21:30:00	\N	f	390
1886	470	수	11:30:00	21:30:00	\N	f	390
1887	470	월	11:30:00	21:30:00	\N	f	390
1888	470	일	11:30:00	21:30:00	\N	f	390
1889	470	토	11:30:00	21:30:00	\N	f	390
1890	470	화	11:30:00	21:30:00	\N	f	390
1891	471	금	11:30:00	21:00:00	\N	f	\N
1892	471	목	11:30:00	21:00:00	\N	f	\N
1893	471	수	11:30:00	21:00:00	\N	f	\N
1894	471	월	11:30:00	21:00:00	\N	f	\N
1895	471	일	11:30:00	21:00:00	\N	f	\N
1896	471	토	11:30:00	21:00:00	\N	f	\N
1897	471	화	11:30:00	21:00:00	\N	f	\N
1898	473	금	11:00:00	21:00:00	\N	f	60
1899	473	목	11:00:00	21:00:00	\N	f	60
1900	473	수	11:00:00	21:00:00	\N	f	60
1901	473	월	11:00:00	21:00:00	\N	f	60
1902	473	일	11:00:00	21:00:00	\N	f	60
1903	473	토	11:00:00	21:00:00	\N	f	60
1904	473	화	11:00:00	21:00:00	\N	f	60
1905	474	금	12:00:00	22:00:00	\N	f	\N
1906	474	목	12:00:00	22:00:00	\N	f	\N
1807	447	금	15:00:00	23:00:00	\N	f	60
1808	447	목	15:00:00	23:00:00	\N	f	60
1809	447	수	15:00:00	23:00:00	\N	f	60
1810	447	월	15:00:00	23:00:00	\N	f	60
407	39	금	09:00:00	18:00:00	\N	f	20
408	39	목	09:00:00	18:00:00	\N	f	20
409	39	수	09:00:00	18:00:00	\N	f	20
410	39	월	09:00:00	18:00:00	\N	f	20
411	39	일	09:00:00	18:00:00	\N	f	20
412	39	토	09:00:00	18:00:00	\N	f	20
413	39	화	\N	\N	정기휴무 (매주 화요일)	f	20
428	53	금	12:00:00	22:00:00	\N	f	\N
429	53	목	12:00:00	22:00:00	\N	f	\N
430	53	수	12:00:00	22:00:00	\N	f	\N
431	53	월	\N	\N	정기휴무 (매주 월요일)	f	\N
432	53	일	\N	\N	정기휴무 (매주 일요일)	f	\N
433	53	토	\N	\N	정기휴무 (매주 토요일)	f	\N
434	53	화	12:00:00	22:00:00	\N	f	\N
435	54	금	17:00:00	23:00:00	\N	f	60
436	54	목	17:00:00	23:00:00	\N	f	60
437	54	수	17:00:00	23:00:00	\N	f	60
438	54	월	\N	\N	정기휴무 (매주 월요일)	f	60
439	54	일	15:00:00	23:00:00	\N	f	60
440	54	토	15:00:00	23:00:00	\N	f	60
441	54	화	\N	\N	정기휴무 (매주 화요일)	f	60
442	55	금	11:30:00	22:00:00	\N	f	420
443	55	목	11:30:00	22:00:00	\N	f	420
444	55	수	11:30:00	22:00:00	\N	f	420
445	55	월	11:30:00	22:00:00	\N	f	420
446	55	일	11:30:00	22:00:00	\N	f	420
447	55	토	11:30:00	22:00:00	\N	f	420
448	55	화	11:30:00	22:00:00	\N	f	420
449	56	금	12:00:00	\N	\N	f	60
450	56	목	11:40:00	23:00:00	\N	f	60
451	56	수	12:00:00	\N	\N	f	60
452	56	월	12:00:00	\N	\N	f	60
453	56	일	12:00:00	\N	\N	f	60
454	56	토	11:40:00	23:00:00	\N	f	60
455	56	화	11:40:00	23:00:00	\N	f	60
456	57	금	11:30:00	20:00:00	\N	f	30
457	57	목	11:30:00	20:00:00	\N	f	30
458	57	수	11:30:00	20:00:00	\N	f	30
459	57	월	\N	\N	정기휴무 (매주 월요일)	f	30
460	57	일	11:30:00	17:00:00	\N	f	30
461	57	토	11:30:00	20:00:00	\N	f	30
462	57	화	11:30:00	20:00:00	\N	f	30
463	58	금	09:30:00	21:00:00	\N	f	60
464	58	목	09:30:00	21:00:00	\N	f	60
465	58	수	09:30:00	21:00:00	\N	f	60
466	58	월	09:30:00	21:00:00	\N	f	60
467	58	일	\N	\N	정기휴무 (매주 일요일)	f	60
468	58	토	09:30:00	20:00:00	\N	f	60
469	58	화	09:30:00	21:00:00	\N	f	60
498	51	금	11:30:00	22:00:00	\N	f	420
499	51	목	11:30:00	22:00:00	\N	f	420
500	51	수	11:30:00	22:00:00	\N	f	420
501	51	월	\N	\N	휴무	f	420
502	51	일	12:00:00	22:00:00	\N	f	420
503	51	토	12:00:00	22:00:00	\N	f	420
504	51	화	\N	\N	휴무	f	420
512	69	금	09:00:00	19:00:00	\N	f	\N
513	69	목	09:00:00	19:00:00	\N	f	\N
514	69	수	09:00:00	19:00:00	\N	f	\N
515	69	월	09:00:00	19:00:00	\N	f	\N
516	69	일	09:00:00	19:00:00	\N	f	\N
517	69	토	09:00:00	19:00:00	\N	f	\N
518	69	화	09:00:00	19:00:00	\N	f	\N
519	70	금	11:30:00	23:00:00	\N	f	\N
520	70	목	11:30:00	23:00:00	\N	f	\N
521	70	수	11:30:00	23:00:00	\N	f	\N
522	70	월	\N	\N	정기휴무 (매주 월요일)	f	\N
523	70	일	11:30:00	23:00:00	\N	f	\N
524	70	토	11:30:00	23:00:00	\N	f	\N
525	70	화	11:30:00	23:00:00	\N	f	\N
526	72	금	11:30:00	21:00:00	\N	f	60
527	72	목	11:30:00	21:00:00	\N	f	60
528	72	수	11:30:00	21:00:00	\N	f	60
529	72	월	11:30:00	21:00:00	\N	f	60
530	72	일	11:30:00	21:00:00	\N	f	60
531	72	토	11:30:00	21:00:00	\N	f	60
532	72	화	11:30:00	21:00:00	\N	f	60
533	76	금	12:00:00	22:00:00	\N	f	\N
534	76	목	12:00:00	22:00:00	\N	f	\N
535	76	수	\N	\N	정기휴무 (매주 수요일)	f	\N
536	76	월	12:00:00	22:00:00	\N	f	\N
537	76	일	12:00:00	22:00:00	\N	f	\N
538	76	토	12:00:00	22:00:00	\N	f	\N
539	76	화	\N	\N	정기휴무 (매주 화요일)	f	\N
540	77	금	11:30:00	19:30:00	\N	f	270
541	77	목	11:30:00	19:30:00	\N	f	270
542	77	수	11:30:00	19:30:00	\N	f	270
543	77	월	11:30:00	14:20:00	\N	f	270
544	77	일	12:00:00	19:30:00	\N	f	270
545	77	토	12:00:00	19:30:00	\N	f	270
546	77	화	11:30:00	19:30:00	\N	f	270
561	89	금	11:30:00	22:00:00	\N	f	390
562	89	목	11:30:00	22:00:00	\N	f	390
563	89	수	11:30:00	22:00:00	\N	f	390
564	89	월	\N	\N	정기휴무 (매주 월요일)	f	390
565	89	일	12:00:00	16:00:00	\N	f	390
566	89	토	12:00:00	22:00:00	\N	f	390
567	89	화	11:30:00	22:00:00	\N	f	390
568	90	금	09:00:00	19:00:00	\N	f	20
569	90	목	09:00:00	19:00:00	\N	f	20
570	90	수	09:00:00	19:00:00	\N	f	20
571	90	월	09:00:00	19:00:00	\N	f	20
572	90	일	09:00:00	15:00:00	\N	f	20
573	90	토	09:00:00	19:00:00	\N	f	20
574	90	화	09:00:00	19:00:00	\N	f	20
575	91	금	10:30:00	\N	\N	f	60
576	91	목	10:30:00	\N	\N	f	60
577	91	수	10:30:00	\N	\N	f	60
578	91	월	10:30:00	\N	\N	f	60
579	91	일	10:30:00	\N	\N	f	60
580	91	토	10:30:00	\N	\N	f	60
581	91	화	\N	\N	정기휴무 (매주 화요일)	f	60
582	92	금	18:00:00	22:00:00	\N	f	\N
583	92	목	18:00:00	22:00:00	\N	f	\N
584	92	수	18:00:00	22:00:00	\N	f	\N
585	92	월	18:00:00	22:00:00	\N	f	\N
586	92	일	\N	\N	정기휴무 (매주 일요일)	f	\N
587	92	토	18:00:00	22:00:00	\N	f	\N
588	92	화	18:00:00	22:00:00	\N	f	\N
589	93	금	17:30:00	22:30:00	\N	f	60
590	93	목	17:30:00	22:30:00	\N	f	60
591	93	수	17:30:00	22:30:00	\N	f	60
592	93	월	17:30:00	22:30:00	\N	f	60
593	93	일	\N	\N	정기휴무 (매주 일요일)	f	60
594	93	토	17:30:00	22:30:00	\N	f	60
595	93	화	17:30:00	22:30:00	\N	f	60
596	94	금	17:00:00	\N	\N	f	\N
597	94	목	17:00:00	\N	\N	f	\N
598	94	수	17:00:00	\N	\N	f	\N
599	94	월	\N	\N	휴무	f	\N
600	94	일	\N	\N	휴무	f	\N
601	94	토	17:00:00	\N	\N	f	\N
602	94	화	17:00:00	\N	\N	f	\N
603	95	금	\N	\N	\N	f	\N
604	95	목	\N	\N	\N	f	\N
605	95	수	\N	\N	\N	f	\N
606	95	월	\N	\N	\N	f	\N
607	95	일	\N	\N	\N	f	\N
608	95	토	\N	\N	\N	f	\N
609	95	화	\N	\N	\N	f	\N
617	108	금	11:40:00	20:00:00	\N	f	\N
618	108	목	11:40:00	20:00:00	\N	f	\N
619	108	수	11:40:00	20:00:00	\N	f	\N
620	108	월	11:40:00	20:00:00	\N	f	\N
621	108	일	\N	\N	정기휴무 (매주 일요일)	f	\N
622	108	토	12:00:00	20:00:00	\N	f	\N
623	108	화	11:40:00	15:00:00	\N	f	\N
624	109	금	12:00:00	21:30:00	\N	f	\N
625	109	목	12:00:00	21:30:00	\N	f	\N
626	109	수	12:00:00	21:30:00	\N	f	\N
627	109	월	12:00:00	21:30:00	\N	f	\N
628	109	일	12:00:00	21:30:00	\N	f	\N
629	109	토	12:00:00	21:30:00	\N	f	\N
630	109	화	12:00:00	21:30:00	\N	f	\N
631	110	금	11:30:00	20:30:00	\N	f	30
632	110	목	11:30:00	20:30:00	\N	f	30
633	110	수	11:30:00	20:30:00	\N	f	30
634	110	월	11:30:00	20:30:00	\N	f	30
635	110	일	\N	\N	정기휴무 (매주 일요일)	f	30
636	110	토	11:30:00	15:00:00	\N	f	30
637	110	화	11:30:00	20:30:00	\N	f	30
638	111	금	10:00:00	19:30:00	\N	f	\N
639	111	목	10:00:00	19:30:00	\N	f	\N
640	111	수	10:00:00	19:30:00	\N	f	\N
641	111	월	\N	\N	정기휴무 (매주 월요일)	f	\N
642	111	일	10:00:00	17:00:00	\N	f	\N
643	111	토	10:00:00	19:30:00	\N	f	\N
644	111	화	10:00:00	19:30:00	\N	f	\N
645	112	금	10:00:00	19:00:00	\N	f	\N
646	112	목	10:00:00	19:00:00	\N	f	\N
647	112	수	10:00:00	19:00:00	\N	f	\N
648	112	월	10:00:00	19:00:00	\N	f	\N
649	112	일	10:00:00	19:00:00	\N	f	\N
650	112	토	10:00:00	19:00:00	\N	f	\N
651	112	화	10:00:00	19:00:00	\N	f	\N
652	113	금	11:00:00	20:30:00	\N	f	\N
653	113	목	11:00:00	20:30:00	\N	f	\N
654	113	수	11:00:00	20:30:00	\N	f	\N
655	113	월	11:00:00	20:30:00	\N	f	\N
656	113	일	11:00:00	20:30:00	\N	f	\N
657	113	토	11:00:00	20:30:00	\N	f	\N
658	113	화	11:00:00	20:30:00	\N	f	\N
659	114	금	11:00:00	20:00:00	\N	f	10
660	114	목	11:00:00	20:00:00	\N	f	10
661	114	수	11:00:00	20:00:00	\N	f	10
662	114	월	\N	\N	정기휴무 (매주 월요일)	f	10
663	114	일	11:00:00	20:00:00	\N	f	10
664	114	토	11:00:00	20:00:00	\N	f	10
665	114	화	11:00:00	20:00:00	\N	f	10
666	116	금	11:00:00	18:00:00	\N	f	30
667	116	목	11:00:00	18:00:00	\N	f	30
668	116	수	11:00:00	18:00:00	\N	f	30
669	116	월	\N	\N	정기휴무 (매주 월요일)	f	30
670	116	일	11:00:00	18:00:00	\N	f	30
671	116	토	11:00:00	18:00:00	\N	f	30
672	116	화	\N	\N	정기휴무 (매주 화요일)	f	30
687	129	금	11:30:00	21:30:00	\N	f	\N
688	129	목	11:30:00	21:30:00	\N	f	\N
689	129	수	11:30:00	21:30:00	\N	f	\N
690	129	월	11:30:00	21:30:00	\N	f	\N
691	129	일	10:00:00	20:00:00	\N	f	\N
692	129	토	10:00:00	20:00:00	\N	f	\N
693	129	화	11:30:00	21:30:00	\N	f	\N
694	130	금	18:30:00	01:00:00	\N	f	60
695	130	목	18:30:00	01:00:00	\N	f	60
696	130	수	18:30:00	01:00:00	\N	f	60
697	130	월	18:30:00	01:00:00	\N	f	60
698	130	일	18:30:00	01:00:00	\N	f	60
699	130	토	18:30:00	01:00:00	\N	f	60
700	130	화	\N	\N	정기휴무 (매주 화요일)	f	60
701	131	금	11:30:00	22:00:00	\N	f	420
702	131	목	11:30:00	22:00:00	\N	f	420
703	131	수	11:30:00	22:00:00	\N	f	420
704	131	월	\N	\N	정기휴무 (매주 월요일)	f	420
705	131	일	11:30:00	22:00:00	\N	f	420
706	131	토	11:30:00	22:00:00	\N	f	420
707	131	화	11:30:00	22:00:00	\N	f	420
708	133	금	11:00:00	18:00:00	\N	f	\N
709	133	목	11:00:00	18:00:00	\N	f	\N
710	133	수	11:00:00	18:00:00	\N	f	\N
711	133	월	\N	\N	정기휴무 (매주 월요일)	f	\N
712	133	일	\N	\N	정기휴무 (매주 일요일)	f	\N
713	133	토	11:00:00	18:00:00	\N	f	\N
714	133	화	11:00:00	18:00:00	\N	f	\N
715	134	금	11:00:00	18:00:00	\N	f	\N
716	134	목	11:00:00	18:00:00	\N	f	\N
717	134	수	11:00:00	18:00:00	\N	f	\N
718	134	월	\N	\N	정기휴무 (매주 월요일)	f	\N
719	134	일	11:00:00	18:00:00	\N	f	\N
720	134	토	11:00:00	18:00:00	\N	f	\N
721	134	화	11:00:00	18:00:00	\N	f	\N
743	146	금	\N	\N	\N	f	\N
744	146	목	\N	\N	\N	f	\N
745	146	수	\N	\N	\N	f	\N
746	146	월	\N	\N	\N	f	\N
747	146	일	\N	\N	\N	f	\N
748	146	토	\N	\N	\N	f	\N
749	146	화	\N	\N	\N	f	\N
750	147	금	10:00:00	19:00:00	\N	f	\N
751	147	목	10:00:00	19:00:00	\N	f	\N
752	147	수	10:00:00	19:00:00	\N	f	\N
753	147	월	10:00:00	19:00:00	\N	f	\N
754	147	일	10:00:00	19:00:00	\N	f	\N
755	147	토	10:00:00	19:00:00	\N	f	\N
756	147	화	10:00:00	19:00:00	\N	f	\N
757	148	금	12:30:00	21:30:00	\N	f	\N
758	148	목	12:30:00	21:30:00	\N	f	\N
759	148	수	12:30:00	21:30:00	\N	f	\N
760	148	월	12:30:00	21:30:00	\N	f	\N
761	148	일	12:30:00	21:30:00	\N	f	\N
762	148	토	12:30:00	21:30:00	\N	f	\N
763	148	화	18:30:00	22:00:00	\N	f	\N
764	149	금	10:00:00	17:00:00	\N	f	\N
765	149	목	10:00:00	17:00:00	\N	f	\N
766	149	수	10:00:00	17:00:00	\N	f	\N
767	149	월	\N	\N	정기휴무 (매주 월요일)	f	\N
768	149	일	\N	\N	정기휴무 (매주 일요일)	f	\N
769	149	토	10:00:00	14:00:00	\N	f	\N
770	149	화	10:00:00	17:00:00	\N	f	\N
771	150	금	12:00:00	19:00:00	\N	f	\N
772	150	목	12:00:00	19:00:00	\N	f	\N
773	150	수	12:00:00	19:00:00	\N	f	\N
774	150	월	12:00:00	19:00:00	\N	f	\N
775	150	일	\N	\N	\N	f	\N
776	150	토	12:00:00	19:00:00	\N	f	\N
777	150	화	12:00:00	19:00:00	\N	f	\N
778	151	금	13:00:00	19:30:00	\N	f	\N
779	151	목	13:00:00	19:30:00	\N	f	\N
780	151	수	13:00:00	19:30:00	\N	f	\N
781	151	월	\N	\N	정기휴무 (매주 월요일)	f	\N
782	151	일	13:00:00	19:30:00	\N	f	\N
783	151	토	13:00:00	21:30:00	\N	f	\N
784	151	화	\N	\N	정기휴무 (매주 화요일)	f	\N
799	206	금	08:00:00	20:30:00	\N	f	\N
800	206	목	08:00:00	20:30:00	\N	f	\N
801	206	수	08:00:00	20:30:00	\N	f	\N
802	206	월	08:00:00	20:30:00	\N	f	\N
803	206	일	09:00:00	20:30:00	\N	f	\N
804	206	토	09:00:00	20:30:00	\N	f	\N
805	206	화	08:00:00	20:30:00	\N	f	\N
806	228	금	09:00:00	22:00:00	\N	f	\N
807	228	목	09:00:00	22:00:00	\N	f	\N
808	228	수	09:00:00	22:00:00	\N	f	\N
809	228	월	09:00:00	22:00:00	\N	f	\N
810	228	일	09:00:00	22:00:00	\N	f	\N
811	228	토	09:00:00	22:00:00	\N	f	\N
812	228	화	09:00:00	22:00:00	\N	f	\N
820	310	금	11:00:00	21:00:00	\N	f	\N
821	310	목	11:00:00	21:00:00	\N	f	\N
822	310	수	11:00:00	21:00:00	\N	f	\N
823	310	월	11:00:00	21:00:00	\N	f	\N
824	310	일	10:00:00	20:00:00	\N	f	\N
825	310	토	10:00:00	20:00:00	\N	f	\N
826	310	화	11:00:00	21:00:00	\N	f	\N
827	333	금	14:00:00	19:00:00	\N	f	\N
828	333	목	14:00:00	19:00:00	\N	f	\N
829	333	수	14:00:00	19:00:00	\N	f	\N
830	333	월	\N	\N	정기휴무 (매주 월요일)	f	\N
831	333	일	\N	\N	정기휴무 (매주 일요일)	f	\N
832	333	토	14:00:00	19:00:00	\N	f	\N
833	333	화	14:00:00	19:00:00	\N	f	\N
834	383	금	11:00:00	20:30:00	\N	f	30
835	383	목	11:00:00	20:30:00	\N	f	30
836	383	수	11:00:00	20:30:00	\N	f	30
837	383	월	11:00:00	20:30:00	\N	f	30
838	383	일	11:00:00	20:30:00	\N	f	30
839	383	토	11:00:00	20:30:00	\N	f	30
840	383	화	11:00:00	20:30:00	\N	f	30
841	407	금	11:00:00	21:30:00	\N	f	\N
842	407	목	11:00:00	21:30:00	\N	f	\N
843	407	수	11:00:00	21:30:00	\N	f	\N
844	407	월	11:00:00	21:30:00	\N	f	\N
845	407	일	11:00:00	21:30:00	\N	f	\N
846	407	토	11:00:00	21:30:00	\N	f	\N
847	407	화	11:00:00	21:30:00	\N	f	\N
848	435	금	11:00:00	21:00:00	\N	f	\N
849	435	목	11:00:00	21:00:00	\N	f	\N
850	435	수	11:00:00	21:00:00	\N	f	\N
851	435	월	11:00:00	21:00:00	\N	f	\N
852	435	일	11:00:00	21:00:00	\N	f	\N
853	435	토	11:00:00	21:00:00	\N	f	\N
854	435	화	11:00:00	21:00:00	\N	f	\N
855	436	금	11:00:00	19:00:00	\N	f	\N
856	436	목	11:00:00	19:00:00	\N	f	\N
857	436	수	11:00:00	19:00:00	\N	f	\N
858	436	월	\N	\N	정기휴무 (매주 월요일)	f	\N
859	436	일	11:00:00	18:00:00	\N	f	\N
860	436	토	11:00:00	18:00:00	\N	f	\N
861	436	화	11:00:00	19:00:00	\N	f	\N
925	117	금	11:00:00	21:30:00	\N	f	30
926	117	목	11:00:00	21:30:00	\N	f	30
927	117	수	11:00:00	21:30:00	\N	f	30
928	117	월	11:00:00	21:30:00	\N	f	30
929	117	일	11:00:00	21:30:00	\N	f	30
930	117	토	11:00:00	21:30:00	\N	f	30
931	117	화	11:00:00	21:30:00	\N	f	30
1002	153	금	13:00:00	19:00:00	\N	f	\N
1003	153	목	13:00:00	19:00:00	\N	f	\N
1004	153	수	\N	\N	정기휴무 (매주 수요일)	f	\N
1005	153	월	13:00:00	19:00:00	\N	f	\N
1006	153	일	13:00:00	19:00:00	\N	f	\N
1007	153	토	13:00:00	19:00:00	\N	f	\N
1008	153	화	\N	\N	정기휴무 (매주 화요일)	f	\N
1009	166	금	10:00:00	19:00:00	\N	f	\N
1010	166	목	10:00:00	19:00:00	\N	f	\N
1011	166	수	10:00:00	19:00:00	\N	f	\N
1012	166	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1013	166	일	10:00:00	19:00:00	\N	f	\N
1014	166	토	10:00:00	19:00:00	\N	f	\N
1015	166	화	10:00:00	19:00:00	\N	f	\N
1016	168	금	11:00:00	21:00:00	\N	f	\N
1017	168	목	11:00:00	18:00:00	\N	f	\N
1018	168	수	11:00:00	18:00:00	\N	f	\N
1019	168	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1020	168	일	11:00:00	21:00:00	\N	f	\N
1021	168	토	11:00:00	21:00:00	\N	f	\N
1022	168	화	11:00:00	18:00:00	\N	f	\N
1023	169	금	09:30:00	20:30:00	\N	f	\N
1024	169	목	09:30:00	20:30:00	\N	f	\N
1025	169	수	09:30:00	20:30:00	\N	f	\N
1026	169	월	09:30:00	20:30:00	\N	f	\N
1027	169	일	09:30:00	20:30:00	\N	f	\N
1028	169	토	09:30:00	20:30:00	\N	f	\N
1029	169	화	09:30:00	20:30:00	\N	f	\N
1030	170	금	09:30:00	22:50:00	\N	f	\N
1031	170	목	09:30:00	22:50:00	\N	f	\N
1032	170	수	09:30:00	22:50:00	\N	f	\N
1033	170	월	09:30:00	22:50:00	\N	f	\N
1034	170	일	09:30:00	22:50:00	\N	f	\N
1035	170	토	09:30:00	22:50:00	\N	f	\N
1036	170	화	09:30:00	22:50:00	\N	f	\N
1037	171	금	09:00:00	21:00:00	\N	f	\N
1038	171	목	09:00:00	21:00:00	\N	f	\N
1039	171	수	09:00:00	21:00:00	\N	f	\N
1040	171	월	09:00:00	21:00:00	\N	f	\N
1041	171	일	12:00:00	18:00:00	\N	f	\N
1042	171	토	09:00:00	21:00:00	\N	f	\N
1043	171	화	09:00:00	21:00:00	\N	f	\N
1065	183	금	10:00:00	22:00:00	\N	f	\N
1066	183	목	10:00:00	22:00:00	\N	f	\N
1067	183	수	10:00:00	22:00:00	\N	f	\N
1068	183	월	10:00:00	22:00:00	\N	f	\N
1069	183	일	10:00:00	22:00:00	\N	f	\N
1070	183	토	10:00:00	22:00:00	\N	f	\N
1071	183	화	10:00:00	22:00:00	\N	f	\N
1072	184	금	09:00:00	21:00:00	\N	f	20
1073	184	목	09:00:00	21:00:00	\N	f	20
1074	184	수	09:00:00	21:00:00	\N	f	20
1075	184	월	09:00:00	21:00:00	\N	f	20
1076	184	일	10:00:00	20:00:00	\N	f	20
1077	184	토	10:00:00	20:00:00	\N	f	20
1078	184	화	09:00:00	21:00:00	\N	f	20
1079	186	금	11:00:00	22:30:00	\N	f	\N
1080	186	목	11:00:00	22:30:00	\N	f	\N
1081	186	수	11:00:00	22:30:00	\N	f	\N
1082	186	월	11:00:00	22:30:00	\N	f	\N
1083	186	일	11:00:00	22:30:00	\N	f	\N
1084	186	토	11:00:00	22:30:00	\N	f	\N
1085	186	화	11:00:00	22:30:00	\N	f	\N
1086	187	금	09:00:00	18:30:00	\N	f	30
1087	187	목	09:00:00	18:30:00	\N	f	30
1088	187	수	09:00:00	18:30:00	\N	f	30
1089	187	월	09:00:00	18:30:00	\N	f	30
1090	187	일	\N	\N	정기휴무 (매주 일요일)	f	30
1091	187	토	10:00:00	18:00:00	\N	f	30
1092	187	화	09:00:00	18:30:00	\N	f	30
1093	188	금	08:00:00	22:00:00	\N	f	\N
1094	188	목	08:00:00	22:00:00	\N	f	\N
1095	188	수	08:00:00	22:00:00	\N	f	\N
1096	188	월	08:00:00	22:00:00	\N	f	\N
1097	188	일	10:00:00	21:30:00	\N	f	\N
1098	188	토	10:00:00	21:30:00	\N	f	\N
1099	188	화	08:00:00	22:00:00	\N	f	\N
1142	203	금	07:00:00	18:00:00	\N	f	\N
1143	203	목	07:00:00	18:00:00	\N	f	\N
1144	203	수	07:00:00	18:00:00	\N	f	\N
1145	203	월	07:00:00	18:00:00	\N	f	\N
1146	203	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1147	203	토	\N	\N	정기휴무 (매주 토요일)	f	\N
1148	203	화	07:00:00	18:00:00	\N	f	\N
1149	204	금	08:30:00	21:00:00	\N	f	60
1150	204	목	08:30:00	19:00:00	\N	f	60
1151	204	수	08:30:00	19:00:00	\N	f	60
1152	204	월	\N	\N	휴무	f	60
1153	204	일	11:00:00	21:00:00	\N	f	60
1154	204	토	11:00:00	21:00:00	\N	f	60
1155	204	화	\N	\N	정기휴무 (매주 화요일)	f	60
1156	205	금	09:00:00	16:00:00	\N	f	\N
1157	205	목	\N	\N	정기휴무 (매주 목요일)	f	\N
1158	205	수	\N	\N	정기휴무 (매주 수요일)	f	\N
1159	205	월	09:00:00	16:00:00	\N	f	\N
1160	205	일	09:00:00	16:00:00	\N	f	\N
1161	205	토	09:00:00	16:00:00	\N	f	\N
1162	205	화	09:00:00	16:00:00	\N	f	\N
1198	223	금	11:00:00	22:00:00	\N	f	60
1199	223	목	11:00:00	22:00:00	\N	f	60
1200	223	수	11:00:00	22:00:00	\N	f	60
1201	223	월	11:00:00	22:00:00	\N	f	60
1202	223	일	11:00:00	22:00:00	\N	f	60
1203	223	토	11:00:00	22:00:00	\N	f	60
1204	223	화	11:00:00	22:00:00	\N	f	60
1205	224	금	11:30:00	21:00:00	\N	f	360
1206	224	목	11:30:00	21:00:00	\N	f	360
1207	224	수	11:30:00	21:00:00	\N	f	360
1208	224	월	11:30:00	15:00:00	\N	f	360
1209	224	일	11:30:00	15:00:00	\N	f	360
1210	224	토	11:30:00	21:00:00	\N	f	360
1211	224	화	\N	\N	정기휴무 (매주 화요일)	f	360
1212	225	금	11:50:00	21:00:00	\N	f	\N
1213	225	목	11:50:00	21:00:00	\N	f	\N
1214	225	수	11:50:00	21:00:00	\N	f	\N
1215	225	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1216	225	일	11:50:00	21:00:00	\N	f	\N
1217	225	토	11:50:00	21:00:00	\N	f	\N
1218	225	화	17:00:00	21:00:00	\N	f	\N
1219	226	금	09:00:00	04:00:00	\N	f	\N
1220	226	목	09:00:00	04:00:00	\N	f	\N
1221	226	수	09:00:00	04:00:00	\N	f	\N
1222	226	월	09:00:00	04:00:00	\N	f	\N
1223	226	일	09:00:00	22:00:00	\N	f	\N
1224	226	토	09:00:00	04:00:00	\N	f	\N
1225	226	화	09:00:00	04:00:00	\N	f	\N
1261	243	금	11:00:00	22:40:00	\N	f	60
1262	243	목	11:00:00	22:40:00	\N	f	60
1263	243	수	11:00:00	22:40:00	\N	f	60
1264	243	월	11:00:00	22:40:00	\N	f	60
1265	243	일	\N	\N	정기휴무 (매주 일요일)	f	60
1266	243	토	15:00:00	22:40:00	\N	f	60
1267	243	화	11:00:00	22:40:00	\N	f	60
1268	244	금	10:00:00	23:00:00	\N	f	\N
1269	244	목	10:00:00	23:00:00	\N	f	\N
1270	244	수	10:00:00	23:00:00	\N	f	\N
1271	244	월	10:00:00	23:00:00	\N	f	\N
1272	244	일	10:00:00	23:00:00	\N	f	\N
1273	244	토	10:00:00	23:00:00	\N	f	\N
1274	244	화	10:00:00	23:00:00	\N	f	\N
1275	245	금	11:20:00	21:00:00	\N	f	\N
1276	245	목	11:20:00	21:00:00	\N	f	\N
1277	245	수	11:20:00	21:00:00	\N	f	\N
1278	245	월	11:20:00	21:00:00	\N	f	\N
1279	245	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1280	245	토	11:20:00	21:00:00	\N	f	\N
1281	245	화	11:20:00	21:00:00	\N	f	\N
1282	246	금	10:40:00	21:30:00	\N	f	390
1283	246	목	10:40:00	21:30:00	\N	f	390
1284	246	수	10:40:00	21:30:00	\N	f	390
1285	246	월	\N	\N	정기휴무 (매주 월요일)	f	390
1286	246	일	10:40:00	21:30:00	\N	f	390
1287	246	토	10:40:00	21:30:00	\N	f	390
1288	246	화	10:40:00	21:30:00	\N	f	390
1324	263	금	11:00:00	22:00:00	\N	f	30
1325	263	목	11:00:00	22:00:00	\N	f	30
1326	263	수	11:00:00	22:00:00	\N	f	30
1327	263	월	11:00:00	22:00:00	\N	f	30
1328	263	일	11:00:00	22:00:00	\N	f	30
1329	263	토	11:00:00	22:00:00	\N	f	30
1330	263	화	11:00:00	22:00:00	\N	f	30
1331	264	금	11:30:00	21:30:00	\N	f	390
1332	264	목	11:30:00	21:30:00	\N	f	390
1333	264	수	11:30:00	21:30:00	\N	f	390
1334	264	월	11:30:00	21:30:00	\N	f	390
1335	264	일	11:30:00	21:30:00	\N	f	390
1336	264	토	11:30:00	21:30:00	\N	f	390
1337	264	화	11:30:00	21:30:00	\N	f	390
1338	265	금	11:00:00	22:00:00	\N	f	420
1339	265	목	11:00:00	22:00:00	\N	f	420
1340	265	수	11:00:00	22:00:00	\N	f	420
1341	265	월	11:00:00	22:00:00	\N	f	420
1342	265	일	11:30:00	22:00:00	\N	f	420
1343	265	토	11:30:00	22:00:00	\N	f	420
1344	265	화	\N	\N	정기휴무 (매주 화요일)	f	420
1345	266	금	10:00:00	20:00:00	\N	f	30
1346	266	목	10:00:00	20:00:00	\N	f	30
1347	266	수	10:00:00	20:00:00	\N	f	30
1348	266	월	10:00:00	20:00:00	\N	f	30
1349	266	일	\N	\N	정기휴무 (매주 일요일)	f	30
1350	266	토	10:00:00	16:00:00	\N	f	30
1351	266	화	10:00:00	20:00:00	\N	f	30
1401	283	금	12:00:00	23:00:00	\N	f	\N
1402	283	목	12:00:00	23:00:00	\N	f	\N
1403	283	수	12:00:00	23:00:00	\N	f	\N
1404	283	월	12:00:00	23:00:00	\N	f	\N
1405	283	일	12:00:00	23:00:00	\N	f	\N
1406	283	토	12:00:00	23:00:00	\N	f	\N
1407	283	화	12:00:00	23:00:00	\N	f	\N
1408	284	금	11:30:00	22:30:00	\N	f	\N
1409	284	목	11:30:00	22:30:00	\N	f	\N
1410	284	수	11:30:00	22:30:00	\N	f	\N
1411	284	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1412	284	일	11:30:00	22:30:00	\N	f	\N
1413	284	토	11:30:00	22:30:00	\N	f	\N
1414	284	화	11:30:00	22:30:00	\N	f	\N
1415	285	금	11:30:00	21:30:00	\N	f	60
1416	285	목	11:30:00	21:30:00	\N	f	60
1417	285	수	11:30:00	21:30:00	\N	f	60
1418	285	월	11:30:00	21:30:00	\N	f	60
1419	285	일	11:00:00	21:30:00	\N	f	60
1420	285	토	11:00:00	21:30:00	\N	f	60
1421	285	화	11:30:00	21:30:00	\N	f	60
1422	297	금	07:00:00	23:00:00	\N	f	\N
1423	297	목	07:00:00	23:00:00	\N	f	\N
1424	297	수	07:00:00	23:00:00	\N	f	\N
1425	297	월	07:00:00	23:00:00	\N	f	\N
1426	297	일	07:00:00	14:00:00	\N	f	\N
1427	297	토	07:00:00	23:00:00	\N	f	\N
1428	297	화	07:00:00	23:00:00	\N	f	\N
1429	298	금	11:00:00	21:00:00	\N	f	\N
1430	298	목	11:00:00	21:00:00	\N	f	\N
1431	298	수	11:00:00	21:00:00	\N	f	\N
1432	298	월	11:00:00	21:00:00	\N	f	\N
1433	298	일	11:00:00	15:00:00	\N	f	\N
1434	298	토	11:00:00	18:00:00	\N	f	\N
1435	298	화	11:00:00	21:00:00	\N	f	\N
1436	301	금	11:00:00	21:30:00	\N	f	60
1437	301	목	11:00:00	21:30:00	\N	f	60
1438	301	수	11:00:00	21:30:00	\N	f	60
1439	301	월	11:00:00	21:30:00	\N	f	60
1440	301	일	\N	\N	정기휴무 (매주 일요일)	f	60
1441	301	토	11:30:00	16:00:00	\N	f	60
1442	301	화	11:00:00	21:30:00	\N	f	60
1443	302	금	11:30:00	23:00:00	\N	f	30
1444	302	목	11:30:00	23:00:00	\N	f	30
1445	302	수	11:30:00	23:00:00	\N	f	30
1446	302	월	11:30:00	23:00:00	\N	f	30
1447	302	일	\N	\N	정기휴무 (매주 일요일)	f	30
1448	302	토	\N	\N	정기휴무 (매주 토요일)	f	30
1449	302	화	11:30:00	23:00:00	\N	f	30
1450	304	금	07:00:00	17:00:00	\N	f	\N
1451	304	목	07:00:00	17:00:00	\N	f	\N
1452	304	수	07:00:00	17:00:00	\N	f	\N
1453	304	월	07:00:00	17:00:00	\N	f	\N
1454	304	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1455	304	토	\N	\N	정기휴무 (매주 토요일)	f	\N
1456	304	화	07:00:00	17:00:00	\N	f	\N
1457	312	금	18:00:00	01:30:00	\N	f	30
1458	312	목	18:00:00	01:00:00	\N	f	30
1459	312	수	18:00:00	01:00:00	\N	f	30
1460	312	월	18:00:00	01:00:00	\N	f	30
1461	312	일	14:00:00	20:00:00	\N	f	30
1462	312	토	14:00:00	01:30:00	\N	f	30
1463	312	화	18:00:00	01:00:00	\N	f	30
1464	313	금	08:00:00	17:00:00	\N	f	10
1465	313	목	08:00:00	17:00:00	\N	f	10
1466	313	수	08:00:00	17:00:00	\N	f	10
1467	313	월	08:00:00	17:00:00	\N	f	10
1468	313	일	10:00:00	18:00:00	\N	f	10
1469	313	토	10:00:00	18:00:00	\N	f	10
1470	313	화	08:00:00	17:00:00	\N	f	10
1471	315	금	18:30:00	02:00:00	\N	f	30
1472	315	목	18:30:00	02:00:00	\N	f	30
1473	315	수	18:30:00	02:00:00	\N	f	30
1474	315	월	18:30:00	02:00:00	\N	f	30
2144	566	목	\N	\N	휴무	f	30
1475	315	일	\N	\N	정기휴무 (매주 일요일)	f	30
1476	315	토	18:30:00	02:00:00	\N	f	30
1477	315	화	18:30:00	02:00:00	\N	f	30
1478	316	금	11:00:00	06:00:00	\N	f	60
1479	316	목	11:00:00	06:00:00	\N	f	60
1480	316	수	11:00:00	06:00:00	\N	f	60
1481	316	월	11:00:00	06:00:00	\N	f	60
1482	316	일	15:00:00	03:00:00	\N	f	60
1483	316	토	14:00:00	06:00:00	\N	f	60
1484	316	화	11:00:00	06:00:00	\N	f	60
1485	317	금	11:30:00	20:00:00	\N	f	240
1486	317	목	11:30:00	20:00:00	\N	f	240
1487	317	수	11:30:00	20:00:00	\N	f	240
1488	317	월	11:30:00	20:00:00	\N	f	240
1489	317	일	11:30:00	20:00:00	\N	f	240
1490	317	토	10:00:00	16:00:00	\N	f	240
1491	317	화	11:30:00	20:00:00	\N	f	240
1492	319	금	18:00:00	\N	\N	f	60
1493	319	목	18:00:00	\N	\N	f	60
1494	319	수	18:00:00	\N	\N	f	60
1495	319	월	\N	\N	정기휴무 (매주 월요일)	f	60
1496	319	일	16:00:00	23:00:00	\N	f	60
1497	319	토	16:00:00	\N	\N	f	60
1498	319	화	18:00:00	\N	\N	f	60
1499	320	금	18:00:00	23:00:00	\N	f	60
1500	320	목	18:00:00	23:00:00	\N	f	60
1501	320	수	18:00:00	23:00:00	\N	f	60
1502	320	월	18:00:00	23:00:00	\N	f	60
1503	320	일	\N	\N	정기휴무 (매주 일요일)	f	60
1504	320	토	\N	\N	정기휴무 (매주 토요일)	f	60
1505	320	화	18:00:00	23:00:00	\N	f	60
1506	321	금	10:00:00	\N	\N	f	120
1507	321	목	10:00:00	\N	\N	f	120
1508	321	수	10:00:00	\N	\N	f	120
1509	321	월	10:00:00	\N	\N	f	120
1510	321	일	11:00:00	\N	\N	f	120
1511	321	토	11:00:00	\N	\N	f	120
1512	321	화	10:00:00	\N	\N	f	120
1513	330	금	12:00:00	18:00:00	\N	f	\N
1514	330	목	12:00:00	18:00:00	\N	f	\N
1515	330	수	12:00:00	18:00:00	\N	f	\N
1516	330	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1517	330	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1518	330	토	12:00:00	18:00:00	\N	f	\N
1519	330	화	12:00:00	18:00:00	\N	f	\N
1520	332	금	12:00:00	18:00:00	\N	f	\N
1521	332	목	12:00:00	18:00:00	\N	f	\N
1522	332	수	12:00:00	18:00:00	\N	f	\N
1523	332	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1524	332	일	\N	\N	\N	f	\N
1525	332	토	12:00:00	18:00:00	\N	f	\N
1526	332	화	12:00:00	18:00:00	\N	f	\N
1527	334	금	\N	\N	\N	f	\N
1528	334	목	\N	\N	\N	f	\N
1529	334	수	\N	\N	\N	f	\N
1530	334	월	\N	\N	\N	f	\N
1531	334	일	\N	\N	\N	f	\N
1532	334	토	\N	\N	\N	f	\N
1533	334	화	\N	\N	\N	f	\N
1534	336	금	13:00:00	19:00:00	\N	f	\N
1535	336	목	13:00:00	19:00:00	\N	f	\N
1536	336	수	13:00:00	19:00:00	\N	f	\N
1537	336	월	13:00:00	19:00:00	\N	f	\N
1538	336	일	13:00:00	19:00:00	\N	f	\N
1539	336	토	13:00:00	19:00:00	\N	f	\N
1540	336	화	13:00:00	19:00:00	\N	f	\N
1541	339	금	\N	\N	정기휴무 (매주 금요일)	f	\N
1542	339	목	14:00:00	20:30:00	\N	f	\N
1543	339	수	10:00:00	17:30:00	\N	f	\N
1544	339	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1545	339	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1546	339	토	13:00:00	15:30:00	\N	f	\N
1547	339	화	\N	\N	정기휴무 (매주 화요일)	f	\N
1548	340	금	00:00:00	\N	\N	f	\N
1549	340	목	00:00:00	\N	\N	f	\N
1550	340	수	00:00:00	\N	\N	f	\N
1551	340	월	00:00:00	\N	\N	f	\N
1552	340	일	00:00:00	\N	\N	f	\N
1553	340	토	00:00:00	\N	\N	f	\N
1554	340	화	00:00:00	\N	\N	f	\N
1555	347	금	12:00:00	21:00:00	\N	f	\N
1556	347	목	\N	\N	정기휴무 (매주 목요일)	f	\N
1557	347	수	12:00:00	21:00:00	\N	f	\N
1558	347	월	\N	\N	정기휴무 (매주 월요일)	f	\N
1559	347	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1560	347	토	12:00:00	21:00:00	\N	f	\N
1561	347	화	12:00:00	21:00:00	\N	f	\N
1562	348	금	11:00:00	17:00:00	\N	f	\N
1563	348	목	11:00:00	17:00:00	\N	f	\N
1564	348	수	11:00:00	17:00:00	\N	f	\N
1565	348	월	11:00:00	17:00:00	\N	f	\N
1566	348	일	11:00:00	17:00:00	\N	f	\N
1567	348	토	11:00:00	17:00:00	\N	f	\N
1568	348	화	11:00:00	17:00:00	\N	f	\N
1569	349	금	10:00:00	20:00:00	\N	f	\N
1570	349	목	10:00:00	20:00:00	\N	f	\N
1571	349	수	\N	\N	정기휴무 (매주 수요일)	f	\N
1572	349	월	10:00:00	20:00:00	\N	f	\N
1573	349	일	10:00:00	20:00:00	\N	f	\N
1574	349	토	10:00:00	20:00:00	\N	f	\N
1575	349	화	10:00:00	20:00:00	\N	f	\N
1576	352	금	11:00:00	18:00:00	\N	f	\N
1577	352	목	11:00:00	18:00:00	\N	f	\N
1578	352	수	11:00:00	18:00:00	\N	f	\N
1579	352	월	11:00:00	18:00:00	\N	f	\N
1580	352	일	11:00:00	18:00:00	\N	f	\N
1581	352	토	11:00:00	18:00:00	\N	f	\N
1582	352	화	11:00:00	18:00:00	\N	f	\N
1583	355	금	10:30:00	19:00:00	\N	f	\N
1584	355	목	10:30:00	19:00:00	\N	f	\N
1585	355	수	10:30:00	19:00:00	\N	f	\N
1586	355	월	10:30:00	19:00:00	\N	f	\N
1587	355	일	10:30:00	19:00:00	\N	f	\N
1588	355	토	10:30:00	19:00:00	\N	f	\N
1589	355	화	10:30:00	19:00:00	\N	f	\N
1590	364	금	\N	\N	\N	f	\N
1591	364	목	\N	\N	\N	f	\N
1592	364	수	\N	\N	\N	f	\N
1593	364	월	\N	\N	\N	f	\N
1594	364	일	\N	\N	\N	f	\N
1595	364	토	\N	\N	\N	f	\N
1596	364	화	\N	\N	\N	f	\N
1597	371	금	09:00:00	18:00:00	\N	f	\N
1598	371	목	09:00:00	18:00:00	\N	f	\N
1599	371	수	09:00:00	18:00:00	\N	f	\N
1600	371	월	09:00:00	18:00:00	\N	f	\N
1601	371	일	09:00:00	18:00:00	\N	f	\N
1602	371	토	09:00:00	18:00:00	\N	f	\N
1603	371	화	09:00:00	18:00:00	\N	f	\N
1604	382	금	10:00:00	20:00:00	\N	f	\N
1605	382	목	10:00:00	20:00:00	\N	f	\N
1606	382	수	10:00:00	20:00:00	\N	f	\N
1607	382	월	10:00:00	20:00:00	\N	f	\N
1608	382	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1609	382	토	\N	\N	정기휴무 (매주 토요일)	f	\N
1610	382	화	10:00:00	20:00:00	\N	f	\N
1611	384	금	10:00:00	19:30:00	\N	f	60
1612	384	목	10:00:00	19:30:00	\N	f	60
1613	384	수	10:00:00	19:30:00	\N	f	60
1614	384	월	10:00:00	19:30:00	\N	f	60
1615	384	일	10:00:00	19:30:00	\N	f	60
1616	384	토	10:00:00	19:30:00	\N	f	60
1617	384	화	10:00:00	19:30:00	\N	f	60
1618	387	금	10:00:00	21:00:00	\N	f	\N
1619	387	목	10:00:00	21:00:00	\N	f	\N
1620	387	수	10:00:00	21:00:00	\N	f	\N
1621	387	월	10:00:00	21:00:00	\N	f	\N
1622	387	일	10:00:00	20:00:00	\N	f	\N
1623	387	토	10:00:00	21:00:00	\N	f	\N
1624	387	화	10:00:00	21:00:00	\N	f	\N
1625	388	금	11:00:00	21:00:00	\N	f	\N
1626	388	목	11:00:00	21:00:00	\N	f	\N
1627	388	수	11:00:00	21:00:00	\N	f	\N
1628	388	월	11:00:00	19:00:00	\N	f	\N
1629	388	일	11:00:00	20:00:00	\N	f	\N
1630	388	토	11:00:00	21:00:00	\N	f	\N
1631	388	화	11:00:00	21:00:00	\N	f	\N
1632	389	금	19:00:00	22:30:00	\N	f	30
1633	389	목	19:00:00	22:30:00	\N	f	30
1634	389	수	19:00:00	22:30:00	\N	f	30
1635	389	월	19:00:00	22:30:00	\N	f	30
1636	389	일	19:00:00	22:30:00	\N	f	30
1637	389	토	18:30:00	23:00:00	\N	f	30
1638	389	화	19:00:00	22:30:00	\N	f	30
1639	392	금	11:00:00	19:00:00	\N	f	60
1640	392	목	11:00:00	19:00:00	\N	f	60
1641	392	수	11:00:00	19:00:00	\N	f	60
1642	392	월	\N	\N	정기휴무 (매주 월요일)	f	60
1643	392	일	11:00:00	19:00:00	\N	f	60
1644	392	토	11:00:00	19:00:00	\N	f	60
1645	392	화	\N	\N	정기휴무 (매주 화요일)	f	60
1646	393	금	10:00:00	18:00:00	\N	f	30
1647	393	목	10:00:00	18:00:00	\N	f	30
1648	393	수	10:00:00	21:00:00	\N	f	30
1649	393	월	10:00:00	18:00:00	\N	f	30
1650	393	일	10:00:00	18:00:00	\N	f	30
1651	393	토	10:00:00	21:00:00	\N	f	30
1652	393	화	10:00:00	18:00:00	\N	f	30
1653	394	금	07:30:00	22:00:00	\N	f	\N
1654	394	목	07:30:00	22:00:00	\N	f	\N
1655	394	수	07:30:00	22:00:00	\N	f	\N
1656	394	월	07:30:00	22:00:00	\N	f	\N
1657	394	일	08:00:00	22:00:00	\N	f	\N
1658	394	토	08:00:00	22:00:00	\N	f	\N
1659	394	화	07:30:00	22:00:00	\N	f	\N
1660	395	금	11:30:00	22:00:00	\N	f	390
1661	395	목	11:30:00	22:00:00	\N	f	390
1662	395	수	11:30:00	22:00:00	\N	f	390
1663	395	월	11:30:00	22:00:00	\N	f	390
1664	395	일	11:30:00	20:00:00	\N	f	390
1665	395	토	11:30:00	22:00:00	\N	f	390
1666	395	화	11:30:00	22:00:00	\N	f	390
1667	396	금	09:30:00	18:00:00	\N	f	60
1668	396	목	09:30:00	18:00:00	\N	f	60
1669	396	수	09:30:00	18:00:00	\N	f	60
1670	396	월	09:30:00	18:00:00	\N	f	60
1671	396	일	\N	\N	정기휴무 (매주 일요일)	f	60
1672	396	토	11:00:00	18:00:00	\N	f	60
1673	396	화	09:30:00	18:00:00	\N	f	60
1674	405	금	09:00:00	22:00:00	\N	f	30
1675	405	목	09:00:00	22:00:00	\N	f	30
1676	405	수	09:00:00	22:00:00	\N	f	30
1677	405	월	09:00:00	22:00:00	\N	f	30
1678	405	일	09:00:00	22:00:00	\N	f	30
1679	405	토	09:00:00	22:00:00	\N	f	30
1680	405	화	09:00:00	22:00:00	\N	f	30
1681	406	금	\N	\N	휴무	f	\N
1682	406	목	\N	\N	휴무	f	\N
1683	406	수	\N	\N	휴무	f	\N
1684	406	월	\N	\N	휴무	f	\N
1685	406	일	\N	\N	휴무	f	\N
1686	406	토	\N	\N	휴무	f	\N
1687	406	화	\N	\N	휴무	f	\N
1688	408	금	11:00:00	20:00:00	\N	f	\N
1689	408	목	11:00:00	20:00:00	\N	f	\N
1690	408	수	11:00:00	20:00:00	\N	f	\N
1691	408	월	11:00:00	20:00:00	\N	f	\N
1692	408	일	11:00:00	20:00:00	\N	f	\N
1693	408	토	11:00:00	20:00:00	\N	f	\N
1694	408	화	11:00:00	20:00:00	\N	f	\N
1695	409	금	12:00:00	18:00:00	\N	f	\N
1696	409	목	12:00:00	18:00:00	\N	f	\N
1697	409	수	\N	\N	정기휴무 (매주 수요일)	f	\N
1698	409	월	12:00:00	18:00:00	\N	f	\N
1699	409	일	12:00:00	18:00:00	\N	f	\N
1700	409	토	12:00:00	19:00:00	\N	f	\N
1701	409	화	\N	\N	정기휴무 (매주 화요일)	f	\N
1702	410	금	13:00:00	19:00:00	\N	f	\N
1703	410	목	13:00:00	19:00:00	\N	f	\N
1704	410	수	13:00:00	19:00:00	\N	f	\N
1705	410	월	\N	\N	휴무	f	\N
1706	410	일	\N	\N	휴무	f	\N
1707	410	토	13:00:00	19:00:00	\N	f	\N
1708	410	화	\N	\N	휴무	f	\N
1709	411	금	\N	\N	\N	f	\N
1710	411	목	\N	\N	\N	f	\N
1711	411	수	\N	\N	\N	f	\N
1712	411	월	\N	\N	\N	f	\N
1713	411	일	\N	\N	\N	f	\N
1714	411	토	\N	\N	\N	f	\N
1715	411	화	\N	\N	\N	f	\N
1716	413	금	11:00:00	21:00:00	\N	f	30
1717	413	목	11:00:00	21:00:00	\N	f	30
1718	413	수	\N	\N	정기휴무 (매주 수요일)	f	30
1719	413	월	11:00:00	21:00:00	\N	f	30
1720	413	일	11:00:00	21:00:00	\N	f	30
1721	413	토	11:00:00	21:00:00	\N	f	30
1722	413	화	\N	\N	정기휴무 (매주 화요일)	f	30
1723	414	금	12:00:00	20:00:00	\N	f	30
1724	414	목	12:00:00	20:00:00	\N	f	30
1725	414	수	12:00:00	18:30:00	\N	f	30
1726	414	월	12:00:00	18:30:00	\N	f	30
1727	414	일	12:00:00	20:30:00	\N	f	30
1728	414	토	12:00:00	20:30:00	\N	f	30
1729	414	화	12:00:00	17:30:00	\N	f	30
1730	415	금	11:00:00	20:00:00	\N	f	\N
1731	415	목	11:00:00	20:00:00	\N	f	\N
1732	415	수	11:00:00	20:00:00	\N	f	\N
1733	415	월	11:00:00	20:00:00	\N	f	\N
1734	415	일	11:00:00	20:00:00	\N	f	\N
1735	415	토	11:00:00	20:00:00	\N	f	\N
1736	415	화	\N	\N	정기휴무 (매주 화요일)	f	\N
1811	447	일	11:00:00	23:00:00	\N	f	60
1812	447	토	11:00:00	23:00:00	\N	f	60
1813	447	화	15:00:00	23:00:00	\N	f	60
1814	448	금	11:00:00	20:00:00	\N	f	\N
1815	448	목	11:00:00	20:00:00	\N	f	\N
1816	448	수	11:00:00	20:00:00	\N	f	\N
1817	448	월	11:00:00	20:00:00	\N	f	\N
1818	448	일	11:00:00	20:00:00	\N	f	\N
1819	448	토	11:00:00	20:00:00	\N	f	\N
1820	448	화	11:00:00	20:00:00	\N	f	\N
1821	449	금	10:30:00	20:00:00	\N	f	300
1822	449	목	10:30:00	20:00:00	\N	f	300
1823	449	수	10:30:00	20:00:00	\N	f	300
1824	449	월	10:30:00	20:00:00	\N	f	300
1825	449	일	10:30:00	20:00:00	\N	f	300
1826	449	토	10:30:00	20:00:00	\N	f	300
1827	449	화	10:30:00	20:00:00	\N	f	300
1828	450	금	09:00:00	16:00:00	\N	f	60
1829	450	목	09:00:00	16:00:00	\N	f	60
1830	450	수	09:00:00	16:00:00	\N	f	60
1831	450	월	09:00:00	16:00:00	\N	f	60
1832	450	일	09:00:00	17:00:00	\N	f	60
1833	450	토	09:00:00	17:00:00	\N	f	60
1834	450	화	09:00:00	16:00:00	\N	f	60
1835	451	금	12:00:00	19:00:00	\N	f	\N
1836	451	목	12:00:00	19:00:00	\N	f	\N
1837	451	수	12:00:00	19:00:00	\N	f	\N
1838	451	월	12:00:00	19:00:00	\N	f	\N
1839	451	일	11:00:00	19:00:00	\N	f	\N
1840	451	토	11:00:00	19:00:00	\N	f	\N
1841	451	화	12:00:00	19:00:00	\N	f	\N
1842	453	금	11:20:00	20:00:00	\N	f	30
1843	453	목	11:20:00	20:00:00	\N	f	30
1844	453	수	11:20:00	20:00:00	\N	f	30
1845	453	월	11:20:00	20:00:00	\N	f	30
1846	453	일	11:00:00	16:00:00	\N	f	30
1847	453	토	11:00:00	16:00:00	\N	f	30
1848	453	화	11:20:00	20:00:00	\N	f	30
1849	455	금	12:00:00	21:00:00	\N	f	360
1850	455	목	12:00:00	21:00:00	\N	f	360
1851	455	수	12:00:00	21:00:00	\N	f	360
1852	455	월	\N	\N	정기휴무 (매주 월요일)	f	360
1853	455	일	\N	\N	정기휴무 (매주 일요일)	f	360
1854	455	토	12:00:00	21:00:00	\N	f	360
1855	455	화	12:00:00	15:00:00	\N	f	360
1856	456	금	12:00:00	22:00:00	\N	f	\N
1857	456	목	12:00:00	22:00:00	\N	f	\N
1907	474	수	12:00:00	22:00:00	\N	f	\N
1908	474	월	12:00:00	22:00:00	\N	f	\N
1909	474	일	\N	\N	정기휴무 (매주 일요일)	f	\N
1910	474	토	\N	\N	정기휴무 (매주 토요일)	f	\N
1911	474	화	12:00:00	22:00:00	\N	f	\N
1912	482	금	10:30:00	21:00:00	\N	f	\N
1913	482	목	10:30:00	21:00:00	\N	f	\N
1914	482	수	10:30:00	21:00:00	\N	f	\N
1915	482	월	10:30:00	21:00:00	\N	f	\N
1916	482	일	10:30:00	21:00:00	\N	f	\N
1917	482	토	10:30:00	21:00:00	\N	f	\N
1918	482	화	10:30:00	21:00:00	\N	f	\N
1919	483	금	11:00:00	21:00:00	\N	f	30
1920	483	목	11:00:00	21:00:00	\N	f	30
1921	483	수	11:00:00	21:00:00	\N	f	30
1922	483	월	11:00:00	21:00:00	\N	f	30
1923	483	일	11:00:00	21:00:00	\N	f	30
1924	483	토	11:00:00	21:00:00	\N	f	30
1925	483	화	11:00:00	21:00:00	\N	f	30
2145	566	수	\N	\N	휴무	f	30
1926	484	금	19:00:00	02:00:00	\N	f	\N
1927	484	목	19:00:00	02:00:00	\N	f	\N
1928	484	수	19:00:00	02:00:00	\N	f	\N
1929	484	월	19:00:00	02:00:00	\N	f	\N
1930	484	일	19:00:00	02:00:00	\N	f	\N
1931	484	토	19:00:00	02:00:00	\N	f	\N
1932	484	화	19:00:00	02:00:00	\N	f	\N
1933	485	금	11:30:00	21:30:00	\N	f	30
1934	485	목	11:30:00	21:30:00	\N	f	30
1935	485	수	11:30:00	21:30:00	\N	f	30
1936	485	월	11:30:00	21:30:00	\N	f	30
1937	485	일	11:30:00	21:30:00	\N	f	30
1938	485	토	11:30:00	21:30:00	\N	f	30
1939	485	화	11:30:00	21:30:00	\N	f	30
1940	486	금	11:00:00	21:30:00	\N	f	60
1941	486	목	11:00:00	21:30:00	\N	f	60
1942	486	수	11:00:00	21:30:00	\N	f	60
1943	486	월	\N	\N	정기휴무 (매주 월요일)	f	60
1944	486	일	11:00:00	16:30:00	\N	f	60
1945	486	토	11:00:00	21:30:00	\N	f	60
1946	486	화	\N	\N	정기휴무 (매주 화요일)	f	60
1947	487	금	12:00:00	22:00:00	\N	f	\N
1948	487	목	12:00:00	22:00:00	\N	f	\N
1949	487	수	12:00:00	22:00:00	\N	f	\N
1950	487	월	12:00:00	22:00:00	\N	f	\N
1951	487	일	12:00:00	22:00:00	\N	f	\N
1952	487	토	12:00:00	22:00:00	\N	f	\N
1953	487	화	12:00:00	22:00:00	\N	f	\N
1954	488	금	09:00:00	18:00:00	\N	f	\N
1955	488	목	09:00:00	18:00:00	\N	f	\N
1956	488	수	\N	\N	정기휴무 (매달 1, 3번째 수요일)	f	\N
1957	488	월	09:00:00	18:00:00	\N	f	\N
1958	488	일	09:00:00	17:00:00	\N	f	\N
1959	488	토	09:00:00	17:00:00	\N	f	\N
1960	488	화	09:00:00	18:00:00	\N	f	\N
1961	489	금	11:00:00	20:00:00	\N	f	\N
1962	489	목	11:00:00	20:00:00	\N	f	\N
1963	489	수	11:00:00	20:00:00	\N	f	\N
1964	489	월	11:00:00	20:00:00	\N	f	\N
1965	489	일	11:00:00	20:00:00	\N	f	\N
1966	489	토	11:00:00	20:00:00	\N	f	\N
1967	489	화	11:00:00	20:00:00	\N	f	\N
1968	491	금	08:30:00	18:00:00	\N	f	\N
1969	491	목	08:30:00	18:00:00	\N	f	\N
1970	491	수	08:30:00	18:00:00	\N	f	\N
1971	491	월	08:30:00	18:00:00	\N	f	\N
1972	491	일	08:30:00	18:00:00	\N	f	\N
1973	491	토	08:30:00	18:00:00	\N	f	\N
1974	491	화	08:30:00	18:00:00	\N	f	\N
1975	501	금	12:00:00	22:00:00	\N	f	\N
1976	501	목	12:00:00	22:00:00	\N	f	\N
1977	501	수	12:00:00	22:00:00	\N	f	\N
1978	501	월	12:00:00	22:00:00	\N	f	\N
1979	501	일	12:00:00	22:00:00	\N	f	\N
1980	501	토	12:00:00	22:00:00	\N	f	\N
1981	501	화	\N	\N	정기휴무 (매주 화요일)	f	\N
1982	502	금	12:00:00	01:00:00	\N	f	60
1983	502	목	12:00:00	17:00:00	\N	f	60
1984	502	수	12:00:00	01:00:00	\N	f	60
1985	502	월	12:00:00	22:00:00	\N	f	60
1986	502	일	12:00:00	01:00:00	\N	f	60
1987	502	토	12:00:00	16:00:00	\N	f	60
1988	502	화	12:00:00	22:00:00	\N	f	60
1989	503	금	17:00:00	23:00:00	\N	f	\N
1990	503	목	17:00:00	23:00:00	\N	f	\N
1991	503	수	17:00:00	23:00:00	\N	f	\N
1992	503	월	17:00:00	23:00:00	\N	f	\N
1993	503	일	17:00:00	23:00:00	\N	f	\N
1994	503	토	17:00:00	23:00:00	\N	f	\N
1995	503	화	17:00:00	23:00:00	\N	f	\N
1996	505	금	09:30:00	18:00:00	\N	f	\N
1997	505	목	09:30:00	18:00:00	\N	f	\N
1998	505	수	09:30:00	18:00:00	\N	f	\N
1999	505	월	09:30:00	18:00:00	\N	f	\N
2000	505	일	\N	\N	\N	f	\N
2001	505	토	\N	\N	\N	f	\N
2002	505	화	09:30:00	18:00:00	\N	f	\N
2003	507	금	09:00:00	23:00:00	\N	f	20
2004	507	목	09:00:00	23:00:00	\N	f	20
2005	507	수	09:00:00	23:00:00	\N	f	20
2006	507	월	09:00:00	19:00:00	\N	f	20
2007	507	일	\N	\N	정기휴무 (매주 일요일)	f	20
2008	507	토	12:00:00	23:00:00	\N	f	20
2009	507	화	09:00:00	23:00:00	\N	f	20
2010	508	금	11:00:00	20:00:00	\N	f	\N
2011	508	목	11:00:00	20:00:00	\N	f	\N
2012	508	수	11:00:00	20:00:00	\N	f	\N
2066	534	금	\N	\N	\N	f	\N
2013	508	월	11:00:00	20:00:00	\N	f	\N
2014	508	일	11:00:00	20:00:00	\N	f	\N
2015	508	토	11:00:00	20:00:00	\N	f	\N
2016	508	화	11:00:00	20:00:00	\N	f	\N
2017	518	금	11:00:00	19:00:00	\N	f	\N
2018	518	목	11:00:00	19:00:00	\N	f	\N
2019	518	수	11:00:00	19:00:00	\N	f	\N
2020	518	월	11:00:00	19:00:00	\N	f	\N
2021	518	일	11:00:00	19:00:00	\N	f	\N
2022	518	토	11:00:00	19:00:00	\N	f	\N
2023	518	화	11:00:00	19:00:00	\N	f	\N
2024	519	금	11:00:00	18:00:00	\N	f	\N
2025	519	목	11:00:00	18:00:00	\N	f	\N
2026	519	수	11:00:00	18:00:00	\N	f	\N
2027	519	월	11:00:00	18:00:00	\N	f	\N
2028	519	일	11:00:00	18:00:00	\N	f	\N
2029	519	토	11:00:00	18:00:00	\N	f	\N
2030	519	화	11:00:00	18:00:00	\N	f	\N
2031	520	금	11:00:00	18:00:00	\N	f	\N
2032	520	목	11:00:00	18:00:00	\N	f	\N
2033	520	수	11:00:00	18:00:00	\N	f	\N
2034	520	월	11:00:00	18:00:00	\N	f	\N
2035	520	일	11:00:00	18:00:00	\N	f	\N
2036	520	토	11:00:00	18:00:00	\N	f	\N
2037	520	화	11:00:00	18:00:00	\N	f	\N
2038	521	금	10:00:00	21:00:00	\N	f	\N
2039	521	목	10:00:00	21:00:00	\N	f	\N
2040	521	수	\N	\N	정기휴무 (매주 수요일)	f	\N
2041	521	월	10:00:00	21:00:00	\N	f	\N
2042	521	일	10:00:00	21:00:00	\N	f	\N
2043	521	토	10:00:00	21:00:00	\N	f	\N
2044	521	화	10:00:00	21:00:00	\N	f	\N
2045	522	금	11:00:00	17:00:00	\N	f	\N
2046	522	목	11:00:00	17:00:00	\N	f	\N
2047	522	수	11:00:00	17:00:00	\N	f	\N
2048	522	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2049	522	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2050	522	토	11:00:00	17:00:00	\N	f	\N
2051	522	화	11:00:00	17:00:00	\N	f	\N
2052	523	금	11:00:00	18:00:00	\N	f	\N
2053	523	목	11:00:00	18:00:00	\N	f	\N
2054	523	수	11:00:00	18:00:00	\N	f	\N
2055	523	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2056	523	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2057	523	토	11:00:00	18:00:00	\N	f	\N
2058	523	화	11:00:00	18:00:00	\N	f	\N
2059	526	금	10:30:00	18:00:00	\N	f	\N
2060	526	목	10:30:00	18:00:00	\N	f	\N
2061	526	수	10:30:00	18:00:00	\N	f	\N
2062	526	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2063	526	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2064	526	토	10:30:00	18:00:00	\N	f	\N
2065	526	화	10:30:00	18:00:00	\N	f	\N
2068	534	수	\N	\N	\N	f	\N
2069	534	월	\N	\N	\N	f	\N
2070	534	일	\N	\N	\N	f	\N
2071	534	토	\N	\N	\N	f	\N
2072	534	화	\N	\N	\N	f	\N
2073	535	금	11:00:00	21:00:00	\N	f	\N
2074	535	목	11:00:00	21:00:00	\N	f	\N
2075	535	수	11:00:00	21:00:00	\N	f	\N
2076	535	월	\N	\N	휴무	f	\N
2077	535	일	11:00:00	21:00:00	\N	f	\N
2078	535	토	11:00:00	21:00:00	\N	f	\N
2079	535	화	11:00:00	21:00:00	\N	f	\N
2080	536	금	11:00:00	18:30:00	\N	f	\N
2081	536	목	11:00:00	18:30:00	\N	f	\N
2082	536	수	11:00:00	18:30:00	\N	f	\N
2083	536	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2084	536	일	11:00:00	18:30:00	\N	f	\N
2085	536	토	11:00:00	18:30:00	\N	f	\N
2086	536	화	11:00:00	18:30:00	\N	f	\N
2087	537	금	11:30:00	22:00:00	\N	f	360
2088	537	목	11:30:00	22:00:00	\N	f	360
2089	537	수	11:30:00	22:00:00	\N	f	360
2090	537	월	11:30:00	22:00:00	\N	f	360
2091	537	일	12:00:00	21:00:00	\N	f	360
2092	537	토	11:30:00	22:00:00	\N	f	360
2093	537	화	11:30:00	22:00:00	\N	f	360
2094	538	금	11:00:00	18:00:00	\N	f	\N
2095	538	목	11:00:00	18:00:00	\N	f	\N
2096	538	수	11:00:00	18:00:00	\N	f	\N
2097	538	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2098	538	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2099	538	토	12:00:00	18:00:00	\N	f	\N
2100	538	화	11:00:00	18:00:00	\N	f	\N
2101	549	금	10:30:00	18:00:00	\N	f	\N
2102	549	목	10:30:00	18:00:00	\N	f	\N
2103	549	수	10:30:00	18:00:00	\N	f	\N
2104	549	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2105	549	일	10:30:00	19:00:00	\N	f	\N
2106	549	토	10:30:00	19:00:00	\N	f	\N
2107	549	화	10:30:00	18:00:00	\N	f	\N
2108	550	금	\N	\N	\N	f	\N
2109	550	목	\N	\N	\N	f	\N
2110	550	수	\N	\N	\N	f	\N
2111	550	월	\N	\N	\N	f	\N
2112	550	일	\N	\N	\N	f	\N
2113	550	토	\N	\N	\N	f	\N
2114	550	화	\N	\N	\N	f	\N
2115	551	금	11:30:00	21:00:00	\N	f	390
2116	551	목	11:30:00	21:00:00	\N	f	390
2117	551	수	11:30:00	21:00:00	\N	f	390
2118	551	월	11:30:00	21:00:00	\N	f	390
2119	551	일	11:00:00	21:00:00	\N	f	390
2120	551	토	11:30:00	21:00:00	\N	f	390
2121	551	화	11:30:00	21:00:00	\N	f	390
2122	556	금	09:00:00	18:00:00	\N	f	\N
2123	556	목	09:00:00	18:00:00	\N	f	\N
2124	556	수	09:00:00	18:00:00	\N	f	\N
2125	556	월	09:00:00	18:00:00	\N	f	\N
2126	556	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2127	556	토	\N	\N	정기휴무 (매주 토요일)	f	\N
2128	556	화	09:00:00	18:00:00	\N	f	\N
2129	564	금	10:10:00	20:00:00	\N	f	\N
2130	564	목	10:10:00	20:00:00	\N	f	\N
2131	564	수	10:10:00	20:00:00	\N	f	\N
2132	564	월	10:10:00	20:00:00	\N	f	\N
2133	564	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2134	564	토	10:10:00	20:00:00	\N	f	\N
2135	564	화	10:10:00	20:00:00	\N	f	\N
2136	565	금	11:00:00	18:50:00	\N	f	30
2137	565	목	11:00:00	18:50:00	\N	f	30
2138	565	수	11:00:00	18:50:00	\N	f	30
2139	565	월	11:00:00	18:50:00	\N	f	30
2140	565	일	11:00:00	18:50:00	\N	f	30
2141	565	토	11:00:00	18:50:00	\N	f	30
2142	565	화	11:00:00	18:50:00	\N	f	30
2143	566	금	\N	\N	휴무	f	30
2146	566	월	\N	\N	휴무	f	30
2147	566	일	13:00:00	19:30:00	\N	f	30
2148	566	토	13:00:00	19:30:00	\N	f	30
2149	566	화	\N	\N	휴무	f	30
2150	567	금	17:00:00	11:30:00	\N	f	\N
2151	567	목	17:00:00	11:30:00	\N	f	\N
2152	567	수	17:00:00	11:30:00	\N	f	\N
2153	567	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2154	567	일	13:00:00	11:30:00	\N	f	\N
2155	567	토	13:00:00	11:30:00	\N	f	\N
2156	567	화	17:00:00	11:30:00	\N	f	\N
2157	569	금	10:00:00	19:00:00	\N	f	30
2158	569	목	10:00:00	19:00:00	\N	f	30
2159	569	수	10:00:00	19:00:00	\N	f	30
2160	569	월	\N	\N	정기휴무 (매주 월요일)	f	30
2161	569	일	10:00:00	19:00:00	\N	f	30
2162	569	토	10:00:00	19:00:00	\N	f	30
2163	569	화	10:00:00	19:00:00	\N	f	30
2164	570	금	11:00:00	18:00:00	\N	f	\N
2165	570	목	11:00:00	18:00:00	\N	f	\N
2166	570	수	11:00:00	18:00:00	\N	f	\N
2167	570	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2168	570	일	11:00:00	18:00:00	\N	f	\N
2169	570	토	11:00:00	18:00:00	\N	f	\N
2170	570	화	11:00:00	18:00:00	\N	f	\N
2171	571	금	12:00:00	\N	\N	f	\N
2172	571	목	12:00:00	\N	\N	f	\N
2173	571	수	12:00:00	\N	\N	f	\N
2174	571	월	12:00:00	\N	\N	f	\N
2175	571	일	12:00:00	\N	\N	f	\N
2176	571	토	12:00:00	\N	\N	f	\N
2177	571	화	12:00:00	\N	\N	f	\N
2178	580	금	08:30:00	19:00:00	\N	f	\N
2179	580	목	08:00:00	19:00:00	\N	f	\N
2180	580	수	08:00:00	19:00:00	\N	f	\N
2181	580	월	08:00:00	19:00:00	\N	f	\N
2182	580	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2183	580	토	08:30:00	19:00:00	\N	f	\N
2184	580	화	08:00:00	19:00:00	\N	f	\N
2185	581	금	\N	\N	\N	f	\N
2186	581	목	\N	\N	\N	f	\N
2187	581	수	\N	\N	\N	f	\N
2188	581	월	\N	\N	\N	f	\N
2189	581	일	\N	\N	\N	f	\N
2190	581	토	\N	\N	\N	f	\N
2191	581	화	\N	\N	\N	f	\N
2192	582	금	11:00:00	17:00:00	\N	f	\N
2193	582	목	11:00:00	17:00:00	\N	f	\N
2194	582	수	11:00:00	17:00:00	\N	f	\N
2195	582	월	11:00:00	17:00:00	\N	f	\N
2196	582	일	11:00:00	18:00:00	\N	f	\N
2197	582	토	11:00:00	18:00:00	\N	f	\N
2198	582	화	11:00:00	17:00:00	\N	f	\N
2199	583	금	12:00:00	22:00:00	\N	f	\N
2200	583	목	12:00:00	22:00:00	\N	f	\N
2201	583	수	12:00:00	22:00:00	\N	f	\N
2202	583	월	12:00:00	22:00:00	\N	f	\N
2203	583	일	\N	\N	\N	f	\N
2204	583	토	10:00:00	21:30:00	\N	f	\N
2205	583	화	12:00:00	22:00:00	\N	f	\N
2206	584	금	\N	\N	휴무	f	\N
2207	584	목	\N	\N	휴무	f	\N
2208	584	수	\N	\N	휴무	f	\N
2209	584	월	\N	\N	휴무	f	\N
2210	584	일	\N	\N	휴무	f	\N
2211	584	토	\N	\N	휴무	f	\N
2212	584	화	\N	\N	휴무	f	\N
2213	585	금	09:00:00	19:00:00	\N	f	\N
2214	585	목	09:00:00	19:00:00	\N	f	\N
2215	585	수	09:00:00	19:00:00	\N	f	\N
2216	585	월	09:00:00	19:00:00	\N	f	\N
2217	585	일	09:00:00	19:00:00	\N	f	\N
2218	585	토	09:00:00	19:00:00	\N	f	\N
2219	585	화	09:00:00	19:00:00	\N	f	\N
2220	586	금	14:00:00	20:00:00	\N	f	30
2221	586	목	14:00:00	20:00:00	\N	f	30
2222	586	수	14:00:00	20:00:00	\N	f	30
2223	586	월	14:00:00	20:00:00	\N	f	30
2224	586	일	10:00:00	21:00:00	\N	f	30
2225	586	토	10:00:00	21:00:00	\N	f	30
2226	586	화	\N	\N	정기휴무 (매주 화요일)	f	30
2227	597	금	\N	\N	정기휴무 (매주 금요일)	f	\N
2228	597	목	14:55:00	15:00:00	\N	f	\N
2229	597	수	\N	\N	정기휴무 (매주 수요일)	f	\N
2230	597	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2231	597	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2232	597	토	\N	\N	정기휴무 (매주 토요일)	f	\N
2233	597	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2234	598	금	08:00:00	19:30:00	\N	f	15
2235	598	목	08:00:00	19:30:00	\N	f	15
2236	598	수	08:00:00	19:30:00	\N	f	15
2237	598	월	08:00:00	19:30:00	\N	f	15
2238	598	일	09:00:00	17:30:00	\N	f	15
2239	598	토	09:00:00	17:30:00	\N	f	15
2240	598	화	08:00:00	19:30:00	\N	f	15
2241	599	금	08:00:00	21:00:00	\N	f	\N
2242	599	목	08:00:00	21:00:00	\N	f	\N
2243	599	수	08:00:00	21:00:00	\N	f	\N
2244	599	월	08:00:00	21:00:00	\N	f	\N
2245	599	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2246	599	토	08:00:00	20:00:00	\N	f	\N
2247	599	화	08:00:00	21:00:00	\N	f	\N
2248	600	금	08:00:00	18:00:00	\N	f	\N
2249	600	목	08:00:00	18:00:00	\N	f	\N
2250	600	수	08:00:00	18:00:00	\N	f	\N
2251	600	월	08:00:00	18:00:00	\N	f	\N
2252	600	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2253	600	토	10:00:00	17:00:00	\N	f	\N
2254	600	화	08:00:00	18:00:00	\N	f	\N
2255	601	금	10:30:00	16:00:00	\N	f	\N
2256	601	목	10:30:00	16:00:00	\N	f	\N
2257	601	수	10:30:00	16:00:00	\N	f	\N
2258	601	월	10:30:00	16:00:00	\N	f	\N
2259	601	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2260	601	토	\N	\N	\N	f	\N
2261	601	화	10:30:00	16:00:00	\N	f	\N
2262	612	금	11:30:00	20:30:00	\N	f	30
2263	612	목	11:30:00	20:30:00	\N	f	30
2264	612	수	11:30:00	20:30:00	\N	f	30
2265	612	월	11:30:00	20:30:00	\N	f	30
2266	612	일	11:30:00	20:30:00	\N	f	30
2267	612	토	11:30:00	20:30:00	\N	f	30
2268	612	화	11:30:00	20:30:00	\N	f	30
2269	613	금	11:20:00	21:00:00	\N	f	300
2270	613	목	11:20:00	21:00:00	\N	f	300
2271	613	수	11:20:00	21:00:00	\N	f	300
2272	613	월	11:20:00	21:00:00	\N	f	300
2273	613	일	11:20:00	20:00:00	\N	f	300
2274	613	토	11:20:00	21:00:00	\N	f	300
2275	613	화	11:20:00	21:00:00	\N	f	300
2276	614	금	11:00:00	21:00:00	\N	f	360
2277	614	목	11:00:00	21:00:00	\N	f	360
2278	614	수	11:00:00	21:00:00	\N	f	360
2279	614	월	\N	\N	휴무	f	360
2280	614	일	11:00:00	21:00:00	\N	f	360
2281	614	토	11:00:00	21:00:00	\N	f	360
2282	614	화	\N	\N	정기휴무 (매주 화요일)	f	360
2283	615	금	11:30:00	20:00:00	\N	f	\N
2284	615	목	11:30:00	20:00:00	\N	f	\N
2285	615	수	11:30:00	20:00:00	\N	f	\N
2286	615	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2287	615	일	11:30:00	20:00:00	\N	f	\N
2288	615	토	11:30:00	20:00:00	\N	f	\N
2289	615	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2290	616	금	11:30:00	20:00:00	\N	f	60
2291	616	목	11:30:00	20:00:00	\N	f	60
2292	616	수	\N	\N	정기휴무 (매주 수요일)	f	60
2293	616	월	11:30:00	20:00:00	\N	f	60
2294	616	일	11:30:00	19:00:00	\N	f	60
2295	616	토	11:30:00	20:00:00	\N	f	60
2296	616	화	11:30:00	20:00:00	\N	f	60
2297	617	금	11:30:00	21:00:00	\N	f	390
2298	617	목	11:30:00	21:00:00	\N	f	390
2299	617	수	11:30:00	21:00:00	\N	f	390
2300	617	월	\N	\N	정기휴무 (매주 월요일)	f	390
2301	617	일	11:30:00	21:00:00	\N	f	390
2302	617	토	11:30:00	21:00:00	\N	f	390
2303	617	화	\N	\N	정기휴무 (매주 화요일)	f	390
2304	618	금	11:30:00	22:00:00	\N	f	\N
2305	618	목	11:30:00	22:00:00	\N	f	\N
2306	618	수	11:30:00	22:00:00	\N	f	\N
2307	618	월	11:30:00	22:00:00	\N	f	\N
2308	618	일	11:30:00	22:00:00	\N	f	\N
2309	618	토	11:30:00	22:00:00	\N	f	\N
2310	618	화	11:30:00	22:00:00	\N	f	\N
2311	619	금	11:00:00	21:00:00	\N	f	\N
2312	619	목	11:00:00	21:00:00	\N	f	\N
2313	619	수	11:00:00	21:00:00	\N	f	\N
2314	619	월	11:00:00	21:00:00	\N	f	\N
2315	619	일	11:00:00	21:00:00	\N	f	\N
2316	619	토	11:00:00	21:00:00	\N	f	\N
2317	619	화	11:00:00	21:00:00	\N	f	\N
2318	620	금	10:00:00	20:00:00	\N	f	\N
2319	620	목	10:00:00	20:00:00	\N	f	\N
2320	620	수	10:00:00	20:00:00	\N	f	\N
2321	620	월	10:00:00	20:00:00	\N	f	\N
2322	620	일	10:00:00	20:00:00	\N	f	\N
2323	620	토	10:00:00	20:00:00	\N	f	\N
2324	620	화	10:00:00	20:00:00	\N	f	\N
2325	631	금	11:00:00	21:30:00	\N	f	\N
2326	631	목	11:00:00	21:30:00	\N	f	\N
2327	631	수	11:00:00	21:30:00	\N	f	\N
2328	631	월	11:00:00	21:30:00	\N	f	\N
2329	631	일	11:00:00	21:30:00	\N	f	\N
2330	631	토	11:00:00	21:30:00	\N	f	\N
2331	631	화	11:00:00	21:30:00	\N	f	\N
2332	632	금	11:30:00	21:30:00	\N	f	\N
2333	632	목	11:30:00	21:30:00	\N	f	\N
2334	632	수	11:30:00	21:30:00	\N	f	\N
2335	632	월	11:30:00	21:30:00	\N	f	\N
2336	632	일	11:30:00	21:30:00	\N	f	\N
2337	632	토	11:30:00	21:30:00	\N	f	\N
2338	632	화	11:30:00	21:30:00	\N	f	\N
2339	633	금	12:00:00	23:00:00	\N	f	\N
2340	633	목	12:00:00	23:00:00	\N	f	\N
2341	633	수	12:00:00	23:00:00	\N	f	\N
2342	633	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2343	633	일	12:00:00	23:00:00	\N	f	\N
2344	633	토	12:00:00	23:00:00	\N	f	\N
2345	633	화	12:00:00	23:00:00	\N	f	\N
2346	637	금	11:00:00	22:00:00	\N	f	\N
2347	637	목	11:00:00	22:00:00	\N	f	\N
2348	637	수	11:00:00	22:00:00	\N	f	\N
2349	637	월	11:00:00	22:00:00	\N	f	\N
2350	637	일	11:00:00	22:00:00	\N	f	\N
2351	637	토	11:00:00	22:00:00	\N	f	\N
2352	637	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2353	638	금	10:00:00	20:00:00	\N	f	\N
2354	638	목	10:00:00	20:00:00	\N	f	\N
2355	638	수	10:00:00	20:00:00	\N	f	\N
2356	638	월	10:00:00	20:00:00	\N	f	\N
2357	638	일	10:00:00	20:00:00	\N	f	\N
2358	638	토	10:00:00	20:00:00	\N	f	\N
2359	638	화	10:00:00	20:00:00	\N	f	\N
2360	640	금	10:00:00	20:00:00	\N	f	\N
2361	640	목	10:00:00	20:00:00	\N	f	\N
2362	640	수	10:00:00	20:00:00	\N	f	\N
2363	640	월	10:00:00	20:00:00	\N	f	\N
2364	640	일	10:00:00	20:00:00	\N	f	\N
2365	640	토	10:00:00	20:00:00	\N	f	\N
2366	640	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2828	461	화	\N	\N	\N	f	\N
2409	132	금	11:00:00	18:00:00	\N	f	\N
2410	132	목	11:00:00	18:00:00	\N	f	\N
2411	132	수	11:00:00	18:00:00	\N	f	\N
2412	132	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2413	132	일	11:00:00	18:00:00	\N	f	\N
2414	132	토	11:00:00	18:00:00	\N	f	\N
2415	132	화	11:00:00	18:00:00	\N	f	\N
2416	135	금	11:00:00	19:00:00	\N	f	\N
2417	135	목	11:00:00	19:00:00	\N	f	\N
2418	135	수	11:00:00	19:00:00	\N	f	\N
2419	135	월	11:00:00	19:00:00	\N	f	\N
2420	135	일	11:00:00	19:00:00	\N	f	\N
2421	135	토	11:00:00	19:00:00	\N	f	\N
2422	135	화	11:00:00	19:00:00	\N	f	\N
2444	172	금	07:30:00	17:30:00	\N	f	\N
2445	172	목	07:30:00	17:30:00	\N	f	\N
2446	172	수	07:30:00	17:30:00	\N	f	\N
2447	172	월	07:30:00	17:30:00	\N	f	\N
2448	172	일	10:00:00	18:30:00	\N	f	\N
2449	172	토	10:00:00	18:30:00	\N	f	\N
2450	172	화	07:30:00	17:30:00	\N	f	\N
2500	208	금	11:00:00	18:00:00	\N	f	\N
2501	208	목	11:00:00	18:00:00	\N	f	\N
2502	208	수	11:00:00	18:00:00	\N	f	\N
2503	208	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2504	208	일	11:00:00	18:00:00	\N	f	\N
2505	208	토	11:00:00	18:00:00	\N	f	\N
2506	208	화	11:00:00	18:00:00	\N	f	\N
2507	209	금	10:00:00	20:00:00	\N	f	\N
2508	209	목	10:00:00	20:00:00	\N	f	\N
2509	209	수	10:00:00	20:00:00	\N	f	\N
2510	209	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2511	209	일	10:40:00	18:00:00	\N	f	\N
2512	209	토	10:40:00	18:30:00	\N	f	\N
2513	209	화	10:00:00	20:00:00	\N	f	\N
2514	210	금	11:00:00	20:00:00	\N	f	\N
2515	210	목	11:00:00	20:00:00	\N	f	\N
2516	210	수	11:00:00	20:00:00	\N	f	\N
2517	210	월	11:00:00	20:00:00	\N	f	\N
2518	210	일	11:00:00	20:00:00	\N	f	\N
2519	210	토	11:00:00	20:00:00	\N	f	\N
2520	210	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2521	211	금	11:30:00	19:30:00	\N	f	30
2522	211	목	11:30:00	20:00:00	\N	f	30
2523	211	수	11:30:00	20:00:00	\N	f	30
2524	211	월	\N	\N	정기휴무 (매주 월요일)	f	30
2525	211	일	11:30:00	20:00:00	\N	f	30
2526	211	토	11:30:00	20:00:00	\N	f	30
2527	211	화	11:30:00	20:00:00	\N	f	30
2528	212	금	10:00:00	22:00:00	\N	f	\N
2529	212	목	10:00:00	22:00:00	\N	f	\N
2530	212	수	10:00:00	22:00:00	\N	f	\N
2531	212	월	10:00:00	22:00:00	\N	f	\N
2532	212	일	10:00:00	22:00:00	\N	f	\N
2533	212	토	10:00:00	22:00:00	\N	f	\N
2534	212	화	10:00:00	22:00:00	\N	f	\N
2535	231	금	11:00:00	21:00:00	\N	f	\N
2536	231	목	11:00:00	21:00:00	\N	f	\N
2537	231	수	11:00:00	21:00:00	\N	f	\N
2538	231	월	11:00:00	21:00:00	\N	f	\N
2539	231	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2540	231	토	\N	\N	정기휴무 (매주 토요일)	f	\N
2541	231	화	11:00:00	21:00:00	\N	f	\N
2570	247	금	11:30:00	22:00:00	\N	f	360
2571	247	목	11:30:00	22:00:00	\N	f	360
2572	247	수	11:30:00	22:00:00	\N	f	360
2573	247	월	11:30:00	22:00:00	\N	f	360
2574	247	일	12:00:00	21:00:00	\N	f	360
2575	247	토	12:00:00	22:00:00	\N	f	360
2576	247	화	11:30:00	22:00:00	\N	f	360
2577	248	금	11:30:00	22:00:00	\N	f	420
2578	248	목	11:30:00	22:00:00	\N	f	420
2579	248	수	11:30:00	22:00:00	\N	f	420
2580	248	월	11:30:00	22:00:00	\N	f	420
2581	248	일	11:30:00	22:00:00	\N	f	420
2582	248	토	11:30:00	22:00:00	\N	f	420
2583	248	화	11:30:00	22:00:00	\N	f	420
2584	267	금	15:00:00	\N	\N	f	60
2585	267	목	15:00:00	\N	\N	f	60
2586	267	수	\N	\N	정기휴무 (매주 수요일)	f	60
2587	267	월	15:00:00	\N	\N	f	60
2588	267	일	\N	\N	휴무	f	60
2589	267	토	15:00:00	\N	\N	f	60
2590	267	화	15:00:00	\N	\N	f	60
2591	268	금	11:30:00	22:00:00	\N	f	\N
2592	268	목	11:30:00	22:00:00	\N	f	\N
2593	268	수	11:30:00	22:00:00	\N	f	\N
2594	268	월	11:30:00	22:00:00	\N	f	\N
2595	268	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2596	268	토	\N	\N	정기휴무 (매주 토요일)	f	\N
2597	268	화	11:30:00	22:00:00	\N	f	\N
2598	269	금	10:30:00	21:00:00	\N	f	\N
2599	269	목	10:30:00	21:00:00	\N	f	\N
2600	269	수	10:30:00	21:00:00	\N	f	\N
2601	269	월	10:30:00	21:00:00	\N	f	\N
2602	269	일	10:30:00	21:00:00	\N	f	\N
2603	269	토	10:30:00	21:00:00	\N	f	\N
2604	269	화	10:30:00	21:00:00	\N	f	\N
2605	270	금	11:30:00	20:00:00	\N	f	300
2606	270	목	11:30:00	20:00:00	\N	f	300
2607	270	수	11:30:00	20:00:00	\N	f	300
2608	270	월	11:30:00	20:00:00	\N	f	300
2609	270	일	11:30:00	20:00:00	\N	f	300
2610	270	토	11:30:00	20:00:00	\N	f	300
2611	270	화	\N	\N	정기휴무 (매주 화요일)	f	300
2612	271	금	11:30:00	22:00:00	\N	f	420
2613	271	목	11:30:00	22:00:00	\N	f	420
2614	271	수	11:30:00	22:00:00	\N	f	420
2615	271	월	11:30:00	22:00:00	\N	f	420
2616	271	일	\N	\N	정기휴무 (매주 일요일)	f	420
2617	271	토	11:30:00	22:00:00	\N	f	420
2618	271	화	11:30:00	22:00:00	\N	f	420
2619	272	금	11:00:00	22:00:00	\N	f	30
2620	272	목	11:00:00	22:00:00	\N	f	30
2621	272	수	11:00:00	22:00:00	\N	f	30
2622	272	월	\N	\N	휴무	f	30
2623	272	일	11:00:00	22:00:00	\N	f	30
2624	272	토	11:00:00	22:00:00	\N	f	30
2625	272	화	\N	\N	정기휴무 (매주 화요일)	f	30
2640	286	금	11:00:00	21:00:00	\N	f	30
2641	286	목	11:00:00	21:00:00	\N	f	30
2642	286	수	11:00:00	21:00:00	\N	f	30
2643	286	월	11:00:00	21:00:00	\N	f	30
2644	286	일	11:30:00	21:00:00	\N	f	30
2645	286	토	11:30:00	21:00:00	\N	f	30
2646	286	화	11:00:00	21:00:00	\N	f	30
2647	307	금	08:00:00	18:00:00	\N	f	60
2648	307	목	08:00:00	18:00:00	\N	f	60
2649	307	수	08:00:00	18:00:00	\N	f	60
2650	307	월	08:00:00	18:00:00	\N	f	60
2651	307	일	09:00:00	17:00:00	\N	f	60
2652	307	토	10:00:00	18:00:00	\N	f	60
2653	307	화	08:00:00	18:00:00	\N	f	60
2654	309	금	11:00:00	\N	\N	f	60
2655	309	목	11:00:00	\N	\N	f	60
2656	309	수	11:00:00	\N	\N	f	60
2657	309	월	11:00:00	\N	\N	f	60
2658	309	일	13:00:00	23:00:00	\N	f	60
2659	309	토	13:00:00	\N	\N	f	60
2660	309	화	11:00:00	\N	\N	f	60
2661	311	금	10:30:00	22:00:00	\N	f	\N
2662	311	목	10:30:00	22:00:00	\N	f	\N
2663	311	수	10:30:00	22:00:00	\N	f	\N
2664	311	월	10:30:00	22:00:00	\N	f	\N
2665	311	일	10:30:00	20:00:00	\N	f	\N
2666	311	토	10:30:00	22:00:00	\N	f	\N
2667	311	화	10:30:00	22:00:00	\N	f	\N
2668	323	금	09:00:00	22:00:00	\N	f	\N
2669	323	목	09:00:00	22:00:00	\N	f	\N
2670	323	수	09:00:00	22:00:00	\N	f	\N
2671	323	월	09:00:00	22:00:00	\N	f	\N
2672	323	일	09:00:00	22:00:00	\N	f	\N
2673	323	토	09:00:00	22:00:00	\N	f	\N
2674	323	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2675	324	금	20:00:00	03:00:00	\N	f	\N
2676	324	목	20:00:00	03:00:00	\N	f	\N
2677	324	수	20:00:00	03:00:00	\N	f	\N
2678	324	월	20:00:00	03:00:00	\N	f	\N
2679	324	일	20:00:00	03:00:00	\N	f	\N
2680	324	토	20:00:00	03:00:00	\N	f	\N
2681	324	화	20:00:00	03:00:00	\N	f	\N
2682	328	금	10:00:00	18:00:00	\N	f	\N
2683	328	목	10:00:00	18:00:00	\N	f	\N
2684	328	수	10:00:00	18:00:00	\N	f	\N
2685	328	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2686	328	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2687	328	토	10:00:00	18:00:00	\N	f	\N
2688	328	화	10:00:00	18:00:00	\N	f	\N
2689	329	금	11:30:00	18:30:00	\N	f	\N
2690	329	목	11:30:00	18:30:00	\N	f	\N
2691	329	수	11:30:00	18:30:00	\N	f	\N
2692	329	월	11:30:00	18:30:00	\N	f	\N
2693	329	일	11:30:00	18:30:00	\N	f	\N
2694	329	토	11:30:00	18:30:00	\N	f	\N
2695	329	화	11:30:00	18:30:00	\N	f	\N
2696	344	금	09:00:00	17:00:00	\N	f	\N
2697	344	목	09:00:00	17:00:00	\N	f	\N
2698	344	수	09:00:00	17:00:00	\N	f	\N
2699	344	월	09:00:00	17:00:00	\N	f	\N
2700	344	일	\N	\N	\N	f	\N
2701	344	토	\N	\N	\N	f	\N
2702	344	화	09:00:00	17:00:00	\N	f	\N
2703	346	금	\N	\N	\N	f	\N
2704	346	목	\N	\N	\N	f	\N
2705	346	수	\N	\N	\N	f	\N
2706	346	월	\N	\N	\N	f	\N
2707	346	일	\N	\N	\N	f	\N
2708	346	토	\N	\N	\N	f	\N
2709	346	화	\N	\N	\N	f	\N
2724	423	금	10:00:00	19:00:00	\N	f	50
2725	423	목	10:00:00	19:00:00	\N	f	50
2726	423	수	10:00:00	19:00:00	\N	f	50
2727	423	월	10:00:00	19:00:00	\N	f	50
2728	423	일	10:00:00	19:00:00	\N	f	50
2729	423	토	10:00:00	19:00:00	\N	f	50
2730	423	화	\N	\N	정기휴무 (매주 화요일)	f	50
2731	404	금	12:00:00	21:30:00	\N	f	\N
2732	404	목	12:00:00	21:30:00	\N	f	\N
2733	404	수	12:00:00	21:30:00	\N	f	\N
2734	404	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2735	404	일	12:00:00	21:30:00	\N	f	\N
2736	404	토	12:00:00	21:30:00	\N	f	\N
2737	404	화	12:00:00	21:30:00	\N	f	\N
2738	419	금	11:00:00	16:00:00	\N	f	\N
2739	419	목	11:00:00	16:00:00	\N	f	\N
2740	419	수	11:00:00	16:00:00	\N	f	\N
2741	419	월	11:00:00	16:00:00	\N	f	\N
2742	419	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2743	419	토	\N	\N	정기휴무 (매주 토요일)	f	\N
2744	419	화	11:00:00	16:00:00	\N	f	\N
2745	422	금	10:00:00	21:00:00	\N	f	60
2746	422	목	10:00:00	21:00:00	\N	f	60
2747	422	수	10:00:00	21:00:00	\N	f	60
2748	422	월	10:00:00	21:00:00	\N	f	60
2749	422	일	10:00:00	21:00:00	\N	f	60
2750	422	토	10:00:00	21:00:00	\N	f	60
2751	422	화	10:00:00	21:00:00	\N	f	60
2752	440	금	11:00:00	21:00:00	\N	f	60
2753	440	목	11:00:00	21:00:00	\N	f	60
2754	440	수	11:00:00	21:00:00	\N	f	60
2755	440	월	11:00:00	21:00:00	\N	f	60
2756	440	일	11:00:00	21:00:00	\N	f	60
2757	440	토	11:00:00	21:00:00	\N	f	60
2758	440	화	11:00:00	21:00:00	\N	f	60
2759	441	금	11:00:00	22:00:00	\N	f	\N
2760	441	목	11:00:00	22:00:00	\N	f	\N
2761	441	수	11:00:00	22:00:00	\N	f	\N
2762	441	월	11:00:00	22:00:00	\N	f	\N
2763	441	일	11:00:00	22:00:00	\N	f	\N
2764	441	토	11:00:00	22:00:00	\N	f	\N
2765	441	화	11:00:00	22:00:00	\N	f	\N
2766	442	금	10:00:00	21:00:00	\N	f	\N
2767	442	목	10:00:00	21:00:00	\N	f	\N
2768	442	수	10:00:00	21:00:00	\N	f	\N
2769	442	월	10:00:00	21:00:00	\N	f	\N
2770	442	일	10:00:00	21:00:00	\N	f	\N
2771	442	토	10:00:00	21:00:00	\N	f	\N
2772	442	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2773	443	금	12:00:00	22:00:00	\N	f	360
2774	443	목	12:00:00	22:00:00	\N	f	360
2775	443	수	12:00:00	22:00:00	\N	f	360
2776	443	월	\N	\N	정기휴무 (매주 월요일)	f	360
2777	443	일	12:00:00	21:00:00	\N	f	360
2778	443	토	12:00:00	22:00:00	\N	f	360
2779	443	화	12:00:00	22:00:00	\N	f	360
2780	444	금	11:00:00	21:30:00	\N	f	\N
2781	444	목	11:00:00	21:30:00	\N	f	\N
2782	444	수	11:00:00	21:30:00	\N	f	\N
2783	444	월	11:00:00	21:30:00	\N	f	\N
2784	444	일	11:00:00	21:30:00	\N	f	\N
2785	444	토	11:00:00	21:30:00	\N	f	\N
2786	444	화	11:00:00	21:30:00	\N	f	\N
2787	445	금	11:30:00	19:30:00	\N	f	240
2788	445	목	11:30:00	19:30:00	\N	f	240
2789	445	수	11:30:00	19:30:00	\N	f	240
2790	445	월	11:30:00	14:30:00	\N	f	240
2791	445	일	11:30:00	19:30:00	\N	f	240
2792	445	토	11:30:00	19:30:00	\N	f	240
2793	445	화	\N	\N	휴무	f	240
2794	457	금	09:00:00	18:00:00	\N	f	\N
2795	457	목	09:00:00	18:00:00	\N	f	\N
2796	457	수	09:00:00	18:00:00	\N	f	\N
2797	457	월	09:00:00	18:00:00	\N	f	\N
2798	457	일	09:00:00	18:00:00	\N	f	\N
2799	457	토	09:00:00	18:00:00	\N	f	\N
2800	457	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2801	458	금	11:30:00	23:55:00	\N	f	535
2802	458	목	11:30:00	23:55:00	\N	f	535
2803	458	수	11:30:00	23:55:00	\N	f	535
2804	458	월	11:30:00	23:55:00	\N	f	535
2805	458	일	\N	\N	정기휴무 (매주 일요일)	f	535
2806	458	토	11:30:00	23:55:00	\N	f	535
2807	458	화	11:30:00	23:55:00	\N	f	535
2808	459	금	10:00:00	18:00:00	\N	f	60
2809	459	목	10:00:00	18:00:00	\N	f	60
2810	459	수	10:00:00	18:00:00	\N	f	60
2811	459	월	10:00:00	18:00:00	\N	f	60
2812	459	일	10:00:00	18:00:00	\N	f	60
2813	459	토	10:00:00	18:00:00	\N	f	60
2814	459	화	10:00:00	18:00:00	\N	f	60
2815	460	금	11:00:00	20:00:00	\N	f	10
2816	460	목	11:00:00	20:00:00	\N	f	10
2817	460	수	11:00:00	20:00:00	\N	f	10
2818	460	월	11:00:00	20:00:00	\N	f	10
2819	460	일	11:00:00	20:00:00	\N	f	10
2820	460	토	11:00:00	20:00:00	\N	f	10
2821	460	화	\N	\N	정기휴무 (매주 화요일)	f	10
2822	461	금	\N	\N	\N	f	\N
2823	461	목	\N	\N	\N	f	\N
2824	461	수	\N	\N	\N	f	\N
2825	461	월	\N	\N	\N	f	\N
2826	461	일	\N	\N	\N	f	\N
2827	461	토	\N	\N	\N	f	\N
2829	462	금	11:00:00	21:00:00	\N	f	30
2830	462	목	11:00:00	21:00:00	\N	f	30
2831	462	수	11:00:00	21:00:00	\N	f	30
2832	462	월	11:00:00	21:00:00	\N	f	30
2833	462	일	11:30:00	22:00:00	\N	f	30
2834	462	토	11:30:00	22:00:00	\N	f	30
2835	462	화	11:00:00	21:00:00	\N	f	30
2836	463	금	11:30:00	23:55:00	\N	f	535
2837	463	목	11:30:00	23:55:00	\N	f	535
2838	463	수	11:30:00	23:55:00	\N	f	535
2839	463	월	11:30:00	23:55:00	\N	f	535
2840	463	일	\N	\N	정기휴무 (매주 일요일)	f	535
2841	463	토	11:30:00	23:55:00	\N	f	535
2842	463	화	11:30:00	23:55:00	\N	f	535
2843	464	금	11:30:00	22:00:00	\N	f	30
2844	464	목	11:30:00	22:00:00	\N	f	30
2845	464	수	11:30:00	22:00:00	\N	f	30
2846	464	월	\N	\N	정기휴무 (매주 월요일)	f	30
2847	464	일	11:30:00	22:00:00	\N	f	30
2848	464	토	11:30:00	22:00:00	\N	f	30
2849	464	화	11:30:00	22:00:00	\N	f	30
2850	481	금	11:30:00	21:30:00	\N	f	360
2851	481	목	11:30:00	21:30:00	\N	f	360
2852	481	수	11:30:00	21:30:00	\N	f	360
2853	481	월	11:30:00	21:30:00	\N	f	360
2854	481	일	11:30:00	22:00:00	\N	f	360
2855	481	토	11:30:00	22:00:00	\N	f	360
2856	481	화	11:30:00	21:30:00	\N	f	360
2857	495	금	11:00:00	21:00:00	\N	f	60
2858	495	목	11:00:00	21:00:00	\N	f	60
2859	495	수	11:00:00	21:00:00	\N	f	60
2860	495	월	11:00:00	21:00:00	\N	f	60
2861	495	일	11:00:00	21:00:00	\N	f	60
2862	495	토	11:00:00	21:00:00	\N	f	60
2863	495	화	\N	\N	정기휴무 (매주 화요일)	f	60
2864	497	금	11:00:00	19:00:00	\N	f	\N
2865	497	목	11:00:00	19:00:00	\N	f	\N
2866	497	수	11:00:00	19:00:00	\N	f	\N
2867	497	월	11:00:00	19:00:00	\N	f	\N
2868	497	일	11:00:00	19:00:00	\N	f	\N
2869	497	토	11:00:00	19:00:00	\N	f	\N
2870	497	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2871	499	금	17:30:00	23:00:00	\N	f	80
2872	499	목	17:30:00	23:00:00	\N	f	80
2873	499	수	17:30:00	23:00:00	\N	f	80
2874	499	월	17:30:00	23:00:00	\N	f	80
2875	499	일	15:00:00	23:00:00	\N	f	80
2876	499	토	15:00:00	23:00:00	\N	f	80
2877	499	화	17:30:00	23:00:00	\N	f	80
2878	544	금	11:00:00	18:00:00	\N	f	\N
2879	544	목	11:00:00	18:00:00	\N	f	\N
2880	544	수	11:00:00	18:00:00	\N	f	\N
2881	544	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2882	544	일	11:00:00	18:00:00	\N	f	\N
2883	544	토	11:00:00	18:00:00	\N	f	\N
2884	544	화	11:00:00	18:00:00	\N	f	\N
2885	547	금	11:30:00	20:00:00	\N	f	\N
2886	547	목	11:30:00	20:00:00	\N	f	\N
2887	547	수	11:30:00	20:00:00	\N	f	\N
2888	547	월	11:30:00	20:00:00	\N	f	\N
2889	547	일	11:30:00	20:00:00	\N	f	\N
2890	547	토	11:30:00	20:00:00	\N	f	\N
2891	547	화	11:30:00	20:00:00	\N	f	\N
2892	512	금	18:00:00	01:00:00	\N	f	30
2893	512	목	18:00:00	01:00:00	\N	f	30
2894	512	수	18:00:00	01:00:00	\N	f	30
2895	512	월	18:00:00	01:00:00	\N	f	30
2896	512	일	\N	\N	정기휴무 (매주 일요일)	f	30
2897	512	토	18:00:00	01:00:00	\N	f	30
2898	512	화	18:00:00	01:00:00	\N	f	30
2899	513	금	17:00:00	01:00:00	\N	f	\N
2900	513	목	18:00:00	\N	\N	f	\N
2901	513	수	18:00:00	\N	\N	f	\N
2902	513	월	18:00:00	\N	\N	f	\N
2903	513	일	17:00:00	\N	\N	f	\N
2904	513	토	17:00:00	\N	\N	f	\N
2905	513	화	18:00:00	\N	\N	f	\N
2906	514	금	12:00:00	18:00:00	\N	f	30
2907	514	목	12:00:00	18:00:00	\N	f	30
2908	514	수	12:00:00	18:00:00	\N	f	30
2909	514	월	12:00:00	18:00:00	\N	f	30
2910	514	일	13:00:00	18:00:00	\N	f	30
2911	514	토	12:00:00	18:00:00	\N	f	30
2912	514	화	12:00:00	18:00:00	\N	f	30
2913	515	금	10:00:00	18:00:00	\N	f	\N
2914	515	목	10:00:00	18:00:00	\N	f	\N
2915	515	수	10:00:00	18:00:00	\N	f	\N
2916	515	월	10:00:00	18:00:00	\N	f	\N
2917	515	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2918	515	토	\N	\N	정기휴무 (매주 토요일)	f	\N
2919	515	화	10:00:00	18:00:00	\N	f	\N
2920	516	금	18:00:00	\N	\N	f	\N
2921	516	목	18:00:00	\N	\N	f	\N
2922	516	수	18:00:00	\N	\N	f	\N
2923	516	월	18:00:00	\N	\N	f	\N
2924	516	일	18:00:00	\N	\N	f	\N
2925	516	토	18:00:00	\N	\N	f	\N
2926	516	화	18:00:00	\N	\N	f	\N
2927	528	금	11:00:00	18:00:00	\N	f	\N
2928	528	목	11:00:00	18:00:00	\N	f	\N
2929	528	수	11:00:00	18:00:00	\N	f	\N
2930	528	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2931	528	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2932	528	토	11:00:00	18:00:00	\N	f	\N
2933	528	화	11:00:00	18:00:00	\N	f	\N
2934	560	금	09:00:00	20:00:00	\N	f	\N
2935	560	목	09:00:00	20:00:00	\N	f	\N
2936	560	수	09:00:00	20:00:00	\N	f	\N
2937	560	월	09:00:00	20:00:00	\N	f	\N
2938	560	일	09:00:00	20:00:00	\N	f	\N
2939	560	토	09:00:00	20:00:00	\N	f	\N
2940	560	화	09:00:00	20:00:00	\N	f	\N
2941	577	금	10:00:00	17:00:00	\N	f	30
2942	577	목	10:00:00	17:00:00	\N	f	30
2943	577	수	10:00:00	17:00:00	\N	f	30
2944	577	월	10:00:00	17:00:00	\N	f	30
2945	577	일	13:00:00	17:00:00	\N	f	30
2946	577	토	11:00:00	18:00:00	\N	f	30
2947	577	화	10:00:00	17:00:00	\N	f	30
2948	578	금	11:00:00	20:00:00	\N	f	20
2949	578	목	11:00:00	20:00:00	\N	f	20
2950	578	수	11:00:00	20:00:00	\N	f	20
2951	578	월	11:00:00	20:00:00	\N	f	20
2952	578	일	\N	\N	정기휴무 (매주 일요일)	f	20
2953	578	토	11:00:00	20:00:00	\N	f	20
2954	578	화	11:00:00	20:00:00	\N	f	20
2955	579	금	10:00:00	20:00:00	\N	f	30
2956	579	목	10:00:00	20:00:00	\N	f	30
2957	579	수	10:00:00	20:00:00	\N	f	30
2958	579	월	\N	\N	정기휴무 (매주 월요일)	f	30
2959	579	일	10:00:00	20:00:00	\N	f	30
2960	579	토	10:00:00	20:00:00	\N	f	30
2961	579	화	10:00:00	20:00:00	\N	f	30
2962	590	금	11:00:00	18:00:00	\N	f	60
2963	590	목	11:00:00	18:00:00	\N	f	60
2964	590	수	11:00:00	18:00:00	\N	f	60
2965	590	월	\N	\N	정기휴무 (매주 월요일)	f	60
2966	590	일	11:00:00	18:00:00	\N	f	60
2967	590	토	11:00:00	18:00:00	\N	f	60
2968	590	화	\N	\N	정기휴무 (매주 화요일)	f	60
2969	591	금	09:00:00	19:00:00	\N	f	60
2970	591	목	09:00:00	19:00:00	\N	f	60
2971	591	수	\N	\N	정기휴무 (매주 수요일)	f	60
2972	591	월	09:00:00	19:00:00	\N	f	60
2973	591	일	09:00:00	19:00:00	\N	f	60
2974	591	토	09:00:00	19:00:00	\N	f	60
2975	591	화	09:00:00	19:00:00	\N	f	60
2976	592	금	\N	\N	정기휴무 (매주 금요일)	f	\N
2977	592	목	\N	\N	정기휴무 (매주 목요일)	f	\N
2978	592	수	\N	\N	정기휴무 (매주 수요일)	f	\N
2979	592	월	\N	\N	정기휴무 (매주 월요일)	f	\N
2980	592	일	\N	\N	정기휴무 (매주 일요일)	f	\N
2981	592	토	11:00:00	21:00:00	\N	f	\N
2982	592	화	\N	\N	정기휴무 (매주 화요일)	f	\N
2983	593	금	11:00:00	18:30:00	\N	f	30
2984	593	목	11:00:00	18:30:00	\N	f	30
2985	593	수	11:00:00	18:30:00	\N	f	30
2986	593	월	11:00:00	18:30:00	\N	f	30
2987	593	일	11:00:00	18:30:00	\N	f	30
2988	593	토	11:00:00	18:30:00	\N	f	30
2989	593	화	\N	\N	정기휴무 (매주 화요일)	f	30
2990	595	금	09:00:00	17:00:00	\N	f	30
2991	595	목	09:00:00	17:00:00	\N	f	30
2992	595	수	09:00:00	17:00:00	\N	f	30
2993	595	월	\N	\N	정기휴무 (매주 월요일)	f	30
2994	595	일	\N	\N	정기휴무 (매주 일요일)	f	30
2995	595	토	\N	\N	정기휴무 (매주 토요일)	f	30
2996	595	화	\N	\N	정기휴무 (매주 화요일)	f	30
2997	596	금	12:00:00	18:00:00	\N	f	30
2998	596	목	\N	\N	휴무	f	30
2999	596	수	\N	\N	휴무	f	30
3000	596	월	\N	\N	휴무	f	30
3001	596	일	12:00:00	18:00:00	\N	f	30
3002	596	토	12:00:00	19:00:00	\N	f	30
3003	596	화	\N	\N	휴무	f	30
3004	605	금	\N	\N	\N	f	\N
3005	605	목	\N	\N	\N	f	\N
3006	605	수	\N	\N	\N	f	\N
3007	605	월	\N	\N	\N	f	\N
3008	605	일	\N	\N	\N	f	\N
3009	605	토	\N	\N	\N	f	\N
3010	605	화	\N	\N	\N	f	\N
3011	607	금	11:00:00	21:00:00	\N	f	45
3012	607	목	11:00:00	21:00:00	\N	f	45
3013	607	수	11:00:00	21:00:00	\N	f	45
3014	607	월	\N	\N	정기휴무 (매주 월요일)	f	45
3015	607	일	11:00:00	21:00:00	\N	f	45
3016	607	토	11:00:00	21:00:00	\N	f	45
3017	607	화	11:00:00	21:00:00	\N	f	45
3018	608	금	11:30:00	22:00:00	\N	f	90
3019	608	목	11:30:00	22:00:00	\N	f	90
3020	608	수	11:30:00	22:00:00	\N	f	90
3021	608	월	\N	\N	정기휴무 (매주 월요일)	f	90
3022	608	일	11:30:00	21:30:00	\N	f	90
3023	608	토	11:30:00	22:00:00	\N	f	90
3024	608	화	11:30:00	22:00:00	\N	f	90
3025	626	금	08:00:00	19:00:00	\N	f	\N
3026	626	목	08:00:00	19:00:00	\N	f	\N
3027	626	수	08:00:00	19:00:00	\N	f	\N
3028	626	월	08:00:00	19:00:00	\N	f	\N
3029	626	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3030	626	토	08:00:00	14:30:00	\N	f	\N
3031	626	화	08:00:00	19:00:00	\N	f	\N
3032	627	금	11:30:00	21:30:00	\N	f	390
3033	627	목	11:30:00	21:30:00	\N	f	390
3034	627	수	11:30:00	21:30:00	\N	f	390
3035	627	월	\N	\N	정기휴무 (매주 월요일)	f	390
3036	627	일	11:30:00	21:30:00	\N	f	390
3037	627	토	11:30:00	21:30:00	\N	f	390
3038	627	화	\N	\N	정기휴무 (매주 화요일)	f	390
3039	628	금	11:30:00	22:00:00	\N	f	420
3040	628	목	11:30:00	22:00:00	\N	f	420
3041	628	수	11:30:00	22:00:00	\N	f	420
3042	628	월	11:30:00	22:00:00	\N	f	420
3043	628	일	11:30:00	22:00:00	\N	f	420
3044	628	토	11:30:00	22:00:00	\N	f	420
3045	628	화	11:30:00	22:00:00	\N	f	420
3046	629	금	\N	\N	\N	f	\N
3047	629	목	\N	\N	\N	f	\N
3048	629	수	\N	\N	정기휴무 (매주 수요일)	f	\N
3049	629	월	\N	\N	\N	f	\N
3050	629	일	\N	\N	\N	f	\N
3051	629	토	\N	\N	\N	f	\N
3052	629	화	\N	\N	\N	f	\N
3053	641	금	18:00:00	23:00:00	\N	f	\N
3054	641	목	18:00:00	23:00:00	\N	f	\N
3055	641	수	18:00:00	23:00:00	\N	f	\N
3056	641	월	18:00:00	23:00:00	\N	f	\N
3057	641	일	18:00:00	23:00:00	\N	f	\N
3058	641	토	18:00:00	23:00:00	\N	f	\N
3059	641	화	18:00:00	23:00:00	\N	f	\N
3060	647	금	12:00:00	19:00:00	\N	f	\N
3061	647	목	12:00:00	19:00:00	\N	f	\N
3062	647	수	12:00:00	19:00:00	\N	f	\N
3063	647	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3064	647	일	12:00:00	19:00:00	\N	f	\N
3065	647	토	12:00:00	19:00:00	\N	f	\N
3066	647	화	\N	\N	정기휴무 (매주 화요일)	f	\N
3067	648	금	10:00:00	18:00:00	\N	f	\N
3068	648	목	10:00:00	18:00:00	\N	f	\N
3069	648	수	10:00:00	18:00:00	\N	f	\N
3070	648	월	10:00:00	18:00:00	\N	f	\N
3071	648	일	10:00:00	18:00:00	\N	f	\N
3072	648	토	10:00:00	18:00:00	\N	f	\N
3073	648	화	10:00:00	18:00:00	\N	f	\N
3074	649	금	10:00:00	18:00:00	\N	f	\N
3075	649	목	10:00:00	18:00:00	\N	f	\N
3076	649	수	10:00:00	18:00:00	\N	f	\N
3077	649	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3078	649	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3079	649	토	10:00:00	18:00:00	\N	f	\N
3080	649	화	10:00:00	18:00:00	\N	f	\N
3081	650	금	10:00:00	17:00:00	\N	f	\N
3082	650	목	10:00:00	17:00:00	\N	f	\N
3083	650	수	10:00:00	17:00:00	\N	f	\N
3084	650	월	10:00:00	17:00:00	\N	f	\N
3085	650	일	10:00:00	17:00:00	\N	f	\N
3086	650	토	10:00:00	17:00:00	\N	f	\N
3087	650	화	10:00:00	17:00:00	\N	f	\N
3088	651	금	11:00:00	18:00:00	\N	f	\N
3089	651	목	11:00:00	18:00:00	\N	f	\N
3090	651	수	11:00:00	18:00:00	\N	f	\N
3091	651	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3092	651	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3093	651	토	11:00:00	17:00:00	\N	f	\N
3094	651	화	11:00:00	18:00:00	\N	f	\N
3095	652	금	11:00:00	22:00:00	\N	f	\N
3096	652	목	11:00:00	22:00:00	\N	f	\N
3097	652	수	\N	\N	정기휴무 (매주 수요일)	f	\N
3098	652	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3099	652	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3100	652	토	11:00:00	22:00:00	\N	f	\N
3101	652	화	\N	\N	정기휴무 (매주 화요일)	f	\N
3102	655	금	\N	\N	정기휴무 (매주 금요일)	f	\N
3103	655	목	10:00:00	22:00:00	\N	f	\N
3104	655	수	10:00:00	22:00:00	\N	f	\N
3105	655	월	10:00:00	22:00:00	\N	f	\N
3106	655	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3107	655	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3108	655	화	10:00:00	22:00:00	\N	f	\N
3109	662	금	\N	\N	\N	f	\N
3110	662	목	\N	\N	\N	f	\N
3111	662	수	\N	\N	\N	f	\N
3112	662	월	\N	\N	\N	f	\N
3113	662	일	\N	\N	\N	f	\N
3114	662	토	\N	\N	\N	f	\N
3115	662	화	\N	\N	\N	f	\N
3116	663	금	\N	\N	\N	f	\N
3117	663	목	\N	\N	\N	f	\N
3118	663	수	\N	\N	\N	f	\N
3119	663	월	\N	\N	\N	f	\N
3120	663	일	\N	\N	\N	f	\N
3121	663	토	\N	\N	\N	f	\N
3122	663	화	\N	\N	\N	f	\N
3123	668	금	12:00:00	19:00:00	\N	f	\N
3124	668	목	12:00:00	19:00:00	\N	f	\N
3125	668	수	12:00:00	19:00:00	\N	f	\N
3126	668	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3127	668	일	11:00:00	19:00:00	\N	f	\N
3128	668	토	11:00:00	19:00:00	\N	f	\N
3129	668	화	12:00:00	19:00:00	\N	f	\N
3130	669	금	09:30:00	21:00:00	\N	f	30
3131	669	목	09:30:00	21:00:00	\N	f	30
3132	669	수	09:30:00	21:00:00	\N	f	30
3133	669	월	09:30:00	21:00:00	\N	f	30
3134	669	일	08:00:00	21:00:00	\N	f	30
3135	669	토	08:00:00	21:00:00	\N	f	30
3136	669	화	09:30:00	21:00:00	\N	f	30
3137	670	금	12:00:00	02:00:00	\N	f	\N
3138	670	목	12:00:00	\N	\N	f	\N
3139	670	수	12:00:00	\N	\N	f	\N
3140	670	월	12:00:00	\N	\N	f	\N
3141	670	일	12:00:00	\N	\N	f	\N
3142	670	토	12:00:00	02:00:00	\N	f	\N
3143	670	화	12:00:00	\N	\N	f	\N
3144	679	금	08:00:00	21:00:00	\N	f	30
3145	679	목	08:00:00	21:00:00	\N	f	30
3146	679	수	08:00:00	21:00:00	\N	f	30
3147	679	월	08:00:00	21:00:00	\N	f	30
3148	679	일	09:00:00	19:00:00	\N	f	30
3149	679	토	08:00:00	19:00:00	\N	f	30
3150	679	화	08:00:00	21:00:00	\N	f	30
3151	680	금	11:00:00	22:00:00	\N	f	30
3152	680	목	11:00:00	22:00:00	\N	f	30
3153	680	수	11:00:00	22:00:00	\N	f	30
3154	680	월	\N	\N	정기휴무 (매주 월요일)	f	30
3155	680	일	11:00:00	22:00:00	\N	f	30
3156	680	토	11:00:00	22:00:00	\N	f	30
3157	680	화	11:00:00	22:00:00	\N	f	30
3158	682	금	10:00:00	22:00:00	\N	f	\N
3159	682	목	10:00:00	22:00:00	\N	f	\N
3160	682	수	10:00:00	22:00:00	\N	f	\N
3161	682	월	10:00:00	22:00:00	\N	f	\N
3162	682	일	10:00:00	22:00:00	\N	f	\N
3163	682	토	10:00:00	22:00:00	\N	f	\N
3164	682	화	10:00:00	22:00:00	\N	f	\N
3165	683	금	\N	\N	휴무	f	\N
3166	683	목	\N	\N	휴무	f	\N
3167	683	수	\N	\N	휴무	f	\N
3168	683	월	\N	\N	휴무	f	\N
3169	683	일	\N	\N	휴무	f	\N
3170	683	토	\N	\N	휴무	f	\N
3171	683	화	\N	\N	휴무	f	\N
3172	684	금	10:00:00	19:00:00	\N	f	\N
3173	684	목	10:00:00	19:00:00	\N	f	\N
3174	684	수	10:00:00	19:00:00	\N	f	\N
3175	684	월	10:00:00	19:00:00	\N	f	\N
3176	684	일	09:00:00	19:00:00	\N	f	\N
3177	684	토	09:00:00	19:00:00	\N	f	\N
3178	684	화	\N	\N	정기휴무 (매주 화요일)	f	\N
3179	687	금	07:00:00	19:00:00	\N	f	\N
3180	687	목	07:00:00	19:00:00	\N	f	\N
3181	687	수	07:00:00	19:00:00	\N	f	\N
3182	687	월	07:00:00	19:00:00	\N	f	\N
3183	687	일	\N	\N	휴무	f	\N
3184	687	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3185	687	화	07:00:00	19:00:00	\N	f	\N
3186	688	금	07:30:00	19:00:00	\N	f	5
3187	688	목	07:30:00	19:00:00	\N	f	5
3188	688	수	07:30:00	19:00:00	\N	f	5
3189	688	월	07:30:00	19:00:00	\N	f	5
3190	688	일	\N	\N	정기휴무 (매주 일요일)	f	5
3191	688	토	09:00:00	18:00:00	\N	f	5
3192	688	화	07:30:00	19:00:00	\N	f	5
3193	697	금	10:00:00	20:00:00	\N	f	\N
3194	697	목	10:00:00	20:00:00	\N	f	\N
3195	697	수	10:00:00	20:00:00	\N	f	\N
3196	697	월	10:00:00	20:00:00	\N	f	\N
3197	697	일	10:00:00	17:00:00	\N	f	\N
3198	697	토	10:00:00	19:00:00	\N	f	\N
3199	697	화	10:00:00	20:00:00	\N	f	\N
3200	698	금	09:00:00	16:30:00	\N	f	40
3201	698	목	09:00:00	16:30:00	\N	f	40
3202	698	수	09:00:00	16:30:00	\N	f	40
3203	698	월	09:00:00	16:30:00	\N	f	40
3204	698	일	09:00:00	16:30:00	\N	f	40
3205	698	토	\N	\N	정기휴무 (매주 토요일)	f	40
3206	698	화	09:00:00	16:30:00	\N	f	40
3207	700	금	13:00:00	18:00:00	\N	f	60
3208	700	목	13:00:00	18:00:00	\N	f	60
3209	700	수	13:00:00	18:00:00	\N	f	60
3210	700	월	\N	\N	정기휴무 (매주 월요일)	f	60
3211	700	일	\N	\N	정기휴무 (매주 일요일)	f	60
3212	700	토	09:30:00	15:00:00	\N	f	60
3213	700	화	13:00:00	18:00:00	\N	f	60
3214	701	금	10:30:00	18:00:00	\N	f	30
3215	701	목	10:30:00	18:00:00	\N	f	30
3216	701	수	10:30:00	18:00:00	\N	f	30
3217	701	월	11:00:00	18:00:00	\N	f	30
3218	701	일	\N	\N	휴무	f	30
3219	701	토	10:30:00	18:00:00	\N	f	30
3220	701	화	10:30:00	18:00:00	\N	f	30
3221	703	금	10:00:00	22:00:00	\N	f	\N
3222	703	목	10:00:00	22:00:00	\N	f	\N
3223	703	수	10:00:00	22:00:00	\N	f	\N
3224	703	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3225	703	일	08:00:00	22:00:00	\N	f	\N
3226	703	토	\N	\N	\N	f	\N
3227	703	화	10:00:00	22:00:00	\N	f	\N
3228	711	금	11:30:00	22:00:00	\N	f	420
3229	711	목	11:30:00	22:00:00	\N	f	420
3230	711	수	11:30:00	22:00:00	\N	f	420
3231	711	월	11:30:00	22:00:00	\N	f	420
3232	711	일	\N	\N	정기휴무 (매주 일요일)	f	420
3233	711	토	11:30:00	22:00:00	\N	f	420
3234	711	화	11:30:00	22:00:00	\N	f	420
3235	713	금	11:00:00	21:00:00	\N	f	\N
3236	713	목	11:00:00	21:00:00	\N	f	\N
3237	713	수	11:00:00	21:00:00	\N	f	\N
3238	713	월	11:00:00	21:00:00	\N	f	\N
3239	713	일	11:00:00	21:00:00	\N	f	\N
3240	713	토	11:00:00	21:00:00	\N	f	\N
3241	713	화	11:00:00	21:00:00	\N	f	\N
3242	714	금	11:00:00	22:00:00	\N	f	\N
3243	714	목	11:00:00	22:00:00	\N	f	\N
3244	714	수	11:00:00	22:00:00	\N	f	\N
3245	714	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3246	714	일	11:00:00	22:00:00	\N	f	\N
3247	714	토	11:00:00	22:00:00	\N	f	\N
3248	714	화	11:00:00	22:00:00	\N	f	\N
3249	715	금	11:30:00	21:00:00	\N	f	90
3250	715	목	11:30:00	21:00:00	\N	f	90
3251	715	수	11:30:00	21:00:00	\N	f	90
3252	715	월	\N	\N	정기휴무 (매주 월요일)	f	90
3253	715	일	11:30:00	21:00:00	\N	f	90
3254	715	토	11:30:00	21:00:00	\N	f	90
3255	715	화	11:30:00	21:00:00	\N	f	90
3256	716	금	11:30:00	21:30:00	\N	f	\N
3257	716	목	11:30:00	21:30:00	\N	f	\N
3258	716	수	11:30:00	21:30:00	\N	f	\N
3259	716	월	11:30:00	21:30:00	\N	f	\N
3260	716	일	11:30:00	21:30:00	\N	f	\N
3261	716	토	11:30:00	21:30:00	\N	f	\N
3262	716	화	11:30:00	21:30:00	\N	f	\N
3263	717	금	11:30:00	21:30:00	\N	f	\N
3264	717	목	11:30:00	21:30:00	\N	f	\N
3265	717	수	11:30:00	21:30:00	\N	f	\N
3266	717	월	11:30:00	21:30:00	\N	f	\N
3267	717	일	11:30:00	21:30:00	\N	f	\N
3268	717	토	11:30:00	21:30:00	\N	f	\N
3269	717	화	11:30:00	21:30:00	\N	f	\N
3270	719	금	12:00:00	23:00:00	\N	f	480
3271	719	목	12:00:00	23:00:00	\N	f	480
3272	719	수	12:00:00	23:00:00	\N	f	480
3273	719	월	12:00:00	23:00:00	\N	f	480
3274	719	일	\N	\N	정기휴무 (매주 일요일)	f	480
3275	719	토	12:00:00	23:00:00	\N	f	480
3276	719	화	12:00:00	23:00:00	\N	f	480
3277	720	금	10:00:00	22:00:00	\N	f	\N
3278	720	목	10:00:00	22:00:00	\N	f	\N
3279	720	수	10:00:00	22:00:00	\N	f	\N
3280	720	월	10:00:00	22:00:00	\N	f	\N
3281	720	일	10:00:00	22:00:00	\N	f	\N
3282	720	토	10:00:00	22:00:00	\N	f	\N
3283	720	화	\N	\N	정기휴무 (매주 화요일)	f	\N
3284	726	금	06:00:00	21:00:00	\N	f	360
3285	726	목	06:00:00	21:00:00	\N	f	360
3286	726	수	06:00:00	21:00:00	\N	f	360
3287	726	월	06:00:00	21:00:00	\N	f	360
3288	726	일	11:00:00	21:00:00	\N	f	360
3289	726	토	10:00:00	21:00:00	\N	f	360
3290	726	화	06:00:00	21:00:00	\N	f	360
3291	728	금	07:00:00	22:00:00	\N	f	\N
3292	728	목	07:00:00	22:00:00	\N	f	\N
3293	728	수	07:00:00	22:00:00	\N	f	\N
3294	728	월	07:00:00	22:00:00	\N	f	\N
3295	728	일	07:00:00	22:00:00	\N	f	\N
3296	728	토	07:00:00	22:00:00	\N	f	\N
3297	728	화	07:00:00	22:00:00	\N	f	\N
3298	730	금	10:00:00	22:00:00	\N	f	60
3299	730	목	10:00:00	22:00:00	\N	f	60
3300	730	수	\N	\N	정기휴무 (매주 수요일)	f	60
3301	730	월	10:00:00	22:00:00	\N	f	60
3302	730	일	10:00:00	22:00:00	\N	f	60
3303	730	토	10:00:00	22:00:00	\N	f	60
3304	730	화	10:00:00	22:00:00	\N	f	60
3305	731	금	11:00:00	14:30:00	\N	f	60
3306	731	목	11:00:00	14:30:00	\N	f	60
3307	731	수	11:00:00	14:30:00	\N	f	60
3308	731	월	11:00:00	14:30:00	\N	f	60
3309	731	일	11:00:00	19:30:00	\N	f	60
3310	731	토	11:00:00	19:30:00	\N	f	60
3311	731	화	11:00:00	14:30:00	\N	f	60
3312	732	금	11:30:00	20:30:00	\N	f	60
3313	732	목	11:30:00	20:30:00	\N	f	60
3314	732	수	11:30:00	20:30:00	\N	f	60
3315	732	월	\N	\N	정기휴무 (매주 월요일)	f	60
3316	732	일	11:30:00	14:30:00	\N	f	60
3317	732	토	11:30:00	20:30:00	\N	f	60
3318	732	화	11:30:00	20:30:00	\N	f	60
3319	734	금	13:50:00	22:00:00	\N	f	\N
3320	734	목	13:50:00	22:00:00	\N	f	\N
3321	734	수	13:50:00	22:00:00	\N	f	\N
3322	734	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3323	734	일	14:00:00	22:00:00	\N	f	\N
3324	734	토	14:00:00	22:00:00	\N	f	\N
3325	734	화	\N	\N	정기휴무 (매주 화요일)	f	\N
3326	735	금	11:00:00	22:00:00	\N	f	\N
3327	735	목	11:00:00	22:00:00	\N	f	\N
3328	735	수	11:00:00	22:00:00	\N	f	\N
3329	735	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3330	735	일	11:00:00	22:00:00	\N	f	\N
3331	735	토	11:00:00	22:00:00	\N	f	\N
3332	735	화	11:00:00	22:00:00	\N	f	\N
3333	745	금	10:00:00	21:00:00	\N	f	60
3334	745	목	10:00:00	21:00:00	\N	f	60
3335	745	수	10:00:00	21:00:00	\N	f	60
3336	745	월	10:00:00	21:00:00	\N	f	60
3337	745	일	09:00:00	21:00:00	\N	f	60
3338	745	토	09:00:00	21:00:00	\N	f	60
3339	745	화	10:00:00	21:00:00	\N	f	60
3340	750	금	11:30:00	18:00:00	\N	f	60
3341	750	목	11:30:00	18:00:00	\N	f	60
3342	750	수	11:30:00	18:00:00	\N	f	60
3343	750	월	\N	\N	정기휴무 (매주 월요일)	f	60
3344	750	일	12:00:00	20:20:00	\N	f	60
3345	750	토	12:00:00	20:20:00	\N	f	60
3346	750	화	11:30:00	18:00:00	\N	f	60
3347	752	금	00:00:00	\N	\N	f	\N
3348	752	목	00:00:00	\N	\N	f	\N
3349	752	수	00:00:00	\N	\N	f	\N
3350	752	월	00:00:00	\N	\N	f	\N
3351	752	일	00:00:00	\N	\N	f	\N
3352	752	토	00:00:00	\N	\N	f	\N
3353	752	화	00:00:00	\N	\N	f	\N
3354	761	금	11:00:00	19:00:00	\N	f	\N
3355	761	목	11:00:00	19:00:00	\N	f	\N
3356	761	수	11:00:00	19:00:00	\N	f	\N
3357	761	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3358	761	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3359	761	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3360	761	화	11:00:00	19:00:00	\N	f	\N
3361	763	금	11:00:00	19:00:00	\N	f	\N
3362	763	목	11:00:00	19:00:00	\N	f	\N
3363	763	수	11:00:00	19:00:00	\N	f	\N
3364	763	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3365	763	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3366	763	토	11:00:00	19:00:00	\N	f	\N
3367	763	화	11:00:00	19:00:00	\N	f	\N
3368	764	금	11:00:00	17:00:00	\N	f	\N
3369	764	목	\N	\N	휴무	f	\N
3370	764	수	\N	\N	휴무	f	\N
3371	764	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3372	764	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3373	764	토	11:00:00	17:00:00	\N	f	\N
3374	764	화	\N	\N	휴무	f	\N
3375	765	금	10:00:00	16:00:00	\N	f	\N
3376	765	목	10:00:00	16:00:00	\N	f	\N
3377	765	수	10:00:00	16:00:00	\N	f	\N
3378	765	월	10:00:00	16:00:00	\N	f	\N
3379	765	일	10:00:00	16:00:00	\N	f	\N
3380	765	토	10:00:00	16:00:00	\N	f	\N
3381	765	화	10:00:00	16:00:00	\N	f	\N
3382	768	금	08:00:00	19:00:00	\N	f	30
3383	768	목	08:00:00	19:00:00	\N	f	30
3384	768	수	08:00:00	19:00:00	\N	f	30
3385	768	월	08:00:00	19:00:00	\N	f	30
3386	768	일	08:00:00	19:00:00	\N	f	30
3387	768	토	08:00:00	19:00:00	\N	f	30
3388	768	화	08:00:00	19:00:00	\N	f	30
3389	770	금	08:00:00	21:00:00	\N	f	\N
3390	770	목	08:00:00	21:00:00	\N	f	\N
3391	770	수	08:00:00	21:00:00	\N	f	\N
3392	770	월	08:00:00	21:00:00	\N	f	\N
3393	770	일	08:00:00	21:00:00	\N	f	\N
3394	770	토	08:00:00	21:00:00	\N	f	\N
3395	770	화	08:00:00	21:00:00	\N	f	\N
3396	775	금	10:00:00	19:00:00	\N	f	\N
3397	775	목	10:00:00	19:00:00	\N	f	\N
3398	775	수	10:00:00	19:00:00	\N	f	\N
3399	775	월	10:00:00	19:00:00	\N	f	\N
3400	775	일	10:00:00	20:00:00	\N	f	\N
3401	775	토	10:00:00	20:00:00	\N	f	\N
3402	775	화	10:00:00	19:00:00	\N	f	\N
3403	778	금	11:30:00	22:00:00	\N	f	\N
3404	778	목	11:30:00	22:00:00	\N	f	\N
3405	778	수	11:30:00	22:00:00	\N	f	\N
3406	778	월	11:30:00	22:00:00	\N	f	\N
3407	778	일	11:30:00	22:00:00	\N	f	\N
3408	778	토	11:30:00	22:00:00	\N	f	\N
3409	778	화	11:30:00	22:00:00	\N	f	\N
3410	779	금	10:00:00	21:00:00	\N	f	\N
3411	779	목	10:00:00	21:00:00	\N	f	\N
3412	779	수	10:00:00	21:00:00	\N	f	\N
3413	779	월	10:00:00	21:00:00	\N	f	\N
3414	779	일	10:00:00	21:00:00	\N	f	\N
3415	779	토	10:00:00	21:00:00	\N	f	\N
3416	779	화	10:00:00	21:00:00	\N	f	\N
3417	781	금	14:00:00	19:00:00	\N	f	\N
3418	781	목	14:00:00	19:00:00	\N	f	\N
3419	781	수	14:00:00	19:00:00	\N	f	\N
3420	781	월	14:00:00	19:00:00	\N	f	\N
3421	781	일	11:00:00	19:00:00	\N	f	\N
3422	781	토	14:00:00	19:00:00	\N	f	\N
3423	781	화	14:00:00	19:00:00	\N	f	\N
3424	794	금	10:00:00	22:00:00	\N	f	\N
3425	794	목	10:00:00	22:00:00	\N	f	\N
3426	794	수	10:00:00	22:00:00	\N	f	\N
3427	794	월	10:00:00	22:00:00	\N	f	\N
3428	794	일	10:00:00	18:00:00	\N	f	\N
3429	794	토	10:00:00	18:00:00	\N	f	\N
3430	794	화	10:00:00	22:00:00	\N	f	\N
3431	795	금	11:00:00	21:30:00	\N	f	\N
3432	795	목	11:00:00	21:30:00	\N	f	\N
3433	795	수	11:00:00	21:30:00	\N	f	\N
3434	795	월	11:00:00	21:30:00	\N	f	\N
3435	795	일	11:00:00	21:30:00	\N	f	\N
3436	795	토	11:00:00	21:30:00	\N	f	\N
3437	795	화	11:00:00	21:30:00	\N	f	\N
3438	796	금	16:30:00	\N	\N	f	30
3439	796	목	16:30:00	\N	\N	f	30
3440	796	수	16:30:00	\N	\N	f	30
3441	796	월	16:30:00	\N	\N	f	30
3442	796	일	\N	\N	정기휴무 (매주 일요일)	f	30
3443	796	토	16:30:00	\N	\N	f	30
3444	796	화	16:30:00	\N	\N	f	30
3445	797	금	09:00:00	21:00:00	\N	f	\N
3446	797	목	09:00:00	21:00:00	\N	f	\N
3447	797	수	09:00:00	21:00:00	\N	f	\N
3448	797	월	09:00:00	21:00:00	\N	f	\N
3449	797	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3450	797	토	09:00:00	21:00:00	\N	f	\N
3451	797	화	09:00:00	21:00:00	\N	f	\N
3452	804	금	09:00:00	22:00:00	\N	f	\N
3453	804	목	09:00:00	22:00:00	\N	f	\N
3454	804	수	09:00:00	22:00:00	\N	f	\N
3455	804	월	09:00:00	22:00:00	\N	f	\N
3456	804	일	09:00:00	22:00:00	\N	f	\N
3457	804	토	09:00:00	22:00:00	\N	f	\N
3458	804	화	09:00:00	22:00:00	\N	f	\N
3459	806	금	11:00:00	21:00:00	\N	f	\N
3460	806	목	11:00:00	21:00:00	\N	f	\N
3461	806	수	11:00:00	21:00:00	\N	f	\N
3462	806	월	11:00:00	21:00:00	\N	f	\N
3463	806	일	11:00:00	21:00:00	\N	f	\N
3464	806	토	11:00:00	21:00:00	\N	f	\N
3465	806	화	11:00:00	21:00:00	\N	f	\N
3466	808	금	11:00:00	22:00:00	\N	f	420
3467	808	목	11:00:00	22:00:00	\N	f	420
3468	808	수	11:00:00	22:00:00	\N	f	420
3469	808	월	11:00:00	22:00:00	\N	f	420
3470	808	일	11:00:00	22:00:00	\N	f	420
3471	808	토	11:00:00	22:00:00	\N	f	420
3472	808	화	11:00:00	22:00:00	\N	f	420
3473	809	금	11:00:00	22:00:00	\N	f	60
3474	809	목	11:00:00	22:00:00	\N	f	60
3475	809	수	11:00:00	22:00:00	\N	f	60
3476	809	월	11:00:00	22:00:00	\N	f	60
3477	809	일	11:00:00	21:00:00	\N	f	60
3478	809	토	11:00:00	22:00:00	\N	f	60
3479	809	화	11:00:00	22:00:00	\N	f	60
3480	810	금	11:00:00	21:30:00	\N	f	\N
3481	810	목	11:00:00	21:30:00	\N	f	\N
3482	810	수	11:00:00	21:30:00	\N	f	\N
3483	810	월	11:00:00	21:30:00	\N	f	\N
3484	810	일	11:00:00	21:30:00	\N	f	\N
3485	810	토	11:00:00	21:30:00	\N	f	\N
3486	810	화	11:00:00	21:30:00	\N	f	\N
3487	811	금	11:00:00	20:00:00	\N	f	270
3488	811	목	11:00:00	20:00:00	\N	f	270
3489	811	수	11:00:00	20:00:00	\N	f	270
3490	811	월	\N	\N	정기휴무 (매주 월요일)	f	270
3491	811	일	11:00:00	18:00:00	\N	f	270
3492	811	토	\N	\N	휴무	f	270
3493	811	화	11:00:00	20:00:00	\N	f	270
3494	812	금	17:00:00	01:00:00	\N	f	60
3495	812	목	17:00:00	01:00:00	\N	f	60
3496	812	수	17:00:00	01:00:00	\N	f	60
3497	812	월	17:00:00	01:00:00	\N	f	60
3498	812	일	\N	\N	정기휴무 (매주 일요일)	f	60
3499	812	토	17:00:00	01:00:00	\N	f	60
3500	812	화	17:00:00	01:00:00	\N	f	60
3501	819	금	19:00:00	01:00:00	\N	f	\N
3502	819	목	19:00:00	01:00:00	\N	f	\N
3503	819	수	19:00:00	01:00:00	\N	f	\N
3504	819	월	19:00:00	01:00:00	\N	f	\N
3505	819	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3506	819	토	19:00:00	01:00:00	\N	f	\N
3507	819	화	19:00:00	01:00:00	\N	f	\N
3508	820	금	10:00:00	20:00:00	\N	f	\N
3509	820	목	10:00:00	20:00:00	\N	f	\N
3510	820	수	10:00:00	20:00:00	\N	f	\N
3511	820	월	10:00:00	20:00:00	\N	f	\N
3512	820	일	10:00:00	20:00:00	\N	f	\N
3513	820	토	10:00:00	20:00:00	\N	f	\N
3514	820	화	10:00:00	20:00:00	\N	f	\N
3515	823	금	12:00:00	22:00:00	\N	f	60
3516	823	목	12:00:00	22:00:00	\N	f	60
3517	823	수	12:00:00	22:00:00	\N	f	60
3518	823	월	12:00:00	22:00:00	\N	f	60
3519	823	일	12:00:00	22:00:00	\N	f	60
3520	823	토	12:00:00	22:00:00	\N	f	60
3521	823	화	12:00:00	22:00:00	\N	f	60
3522	824	금	\N	\N	\N	f	\N
3523	824	목	\N	\N	\N	f	\N
3524	824	수	\N	\N	\N	f	\N
3525	824	월	\N	\N	\N	f	\N
3526	824	일	\N	\N	\N	f	\N
3527	824	토	\N	\N	\N	f	\N
3528	824	화	\N	\N	\N	f	\N
3529	825	금	14:00:00	18:00:00	\N	f	\N
3530	825	목	10:00:00	12:00:00	\N	f	\N
3531	825	수	14:00:00	18:00:00	\N	f	\N
3532	825	월	14:00:00	18:00:00	\N	f	\N
3533	825	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3534	825	토	10:00:00	12:00:00	\N	f	\N
3535	825	화	14:00:00	18:00:00	\N	f	\N
3536	827	금	13:00:00	19:00:00	\N	f	\N
3537	827	목	13:00:00	19:00:00	\N	f	\N
3538	827	수	13:00:00	19:00:00	\N	f	\N
3539	827	월	13:00:00	19:00:00	\N	f	\N
3540	827	일	13:00:00	19:00:00	\N	f	\N
3541	827	토	13:00:00	19:00:00	\N	f	\N
3542	827	화	13:00:00	19:00:00	\N	f	\N
3543	838	금	\N	\N	\N	f	\N
3544	838	목	\N	\N	\N	f	\N
3545	838	수	\N	\N	\N	f	\N
3546	838	월	\N	\N	\N	f	\N
3547	838	일	\N	\N	\N	f	\N
3548	838	토	\N	\N	\N	f	\N
3549	838	화	\N	\N	\N	f	\N
3550	851	금	08:00:00	18:00:00	\N	f	\N
3551	851	목	08:00:00	18:00:00	\N	f	\N
3552	851	수	08:00:00	18:00:00	\N	f	\N
3553	851	월	08:00:00	18:00:00	\N	f	\N
3554	851	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3555	851	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3556	851	화	08:00:00	18:00:00	\N	f	\N
3557	853	금	12:00:00	19:00:00	\N	f	\N
3558	853	목	\N	\N	정기휴무 (매주 목요일)	f	\N
3559	853	수	11:00:00	17:30:00	\N	f	\N
3560	853	월	11:00:00	17:30:00	\N	f	\N
3561	853	일	11:00:00	17:30:00	\N	f	\N
3562	853	토	12:00:00	19:00:00	\N	f	\N
3563	853	화	11:00:00	17:30:00	\N	f	\N
3564	855	금	08:30:00	20:20:00	\N	f	10
3565	855	목	08:30:00	20:20:00	\N	f	10
3566	855	수	08:30:00	20:20:00	\N	f	10
3567	855	월	08:30:00	20:20:00	\N	f	10
3568	855	일	10:00:00	20:20:00	\N	f	10
3569	855	토	10:00:00	20:20:00	\N	f	10
3570	855	화	08:30:00	20:20:00	\N	f	10
3571	856	금	16:00:00	01:00:00	\N	f	\N
3572	856	목	16:00:00	01:00:00	\N	f	\N
3573	856	수	16:00:00	01:00:00	\N	f	\N
3574	856	월	16:00:00	01:00:00	\N	f	\N
3575	856	일	16:00:00	01:00:00	\N	f	\N
3576	856	토	16:00:00	01:00:00	\N	f	\N
3577	856	화	16:00:00	01:00:00	\N	f	\N
3578	857	금	10:30:00	\N	\N	f	\N
3579	857	목	10:30:00	\N	\N	f	\N
3580	857	수	10:30:00	\N	\N	f	\N
3581	857	월	10:30:00	\N	\N	f	\N
3582	857	일	10:30:00	\N	\N	f	\N
3583	857	토	10:30:00	\N	\N	f	\N
3584	857	화	10:30:00	\N	\N	f	\N
3585	862	금	10:00:00	22:30:00	\N	f	\N
4248	9	수	\N	\N	\N	f	\N
3586	862	목	10:00:00	22:30:00	\N	f	\N
3587	862	수	10:00:00	22:30:00	\N	f	\N
3588	862	월	10:00:00	22:30:00	\N	f	\N
3589	862	일	10:00:00	22:30:00	\N	f	\N
3590	862	토	10:00:00	22:30:00	\N	f	\N
3591	862	화	10:00:00	22:30:00	\N	f	\N
3592	863	금	11:30:00	22:00:00	\N	f	420
3593	863	목	11:30:00	22:00:00	\N	f	420
3594	863	수	11:30:00	22:00:00	\N	f	420
3595	863	월	11:30:00	22:00:00	\N	f	420
3596	863	일	\N	\N	정기휴무 (매주 일요일)	f	420
3597	863	토	\N	\N	정기휴무 (매주 토요일)	f	420
3598	863	화	11:30:00	22:00:00	\N	f	420
3599	866	금	10:30:00	22:00:00	\N	f	\N
3600	866	목	10:30:00	22:00:00	\N	f	\N
3601	866	수	10:30:00	22:00:00	\N	f	\N
3602	866	월	10:30:00	22:00:00	\N	f	\N
3603	866	일	10:30:00	22:00:00	\N	f	\N
3604	866	토	10:30:00	22:00:00	\N	f	\N
3605	866	화	10:30:00	22:00:00	\N	f	\N
3606	870	금	11:30:00	22:00:00	\N	f	480
3607	870	목	11:30:00	22:00:00	\N	f	480
3608	870	수	11:30:00	22:00:00	\N	f	480
3609	870	월	11:30:00	22:00:00	\N	f	480
3610	870	일	\N	\N	정기휴무 (매주 일요일)	f	480
3611	870	토	\N	\N	정기휴무 (매주 토요일)	f	480
3612	870	화	11:30:00	22:00:00	\N	f	480
3613	893	금	\N	\N	정기휴무 (매주 금요일)	f	\N
3614	893	목	\N	\N	\N	f	\N
3615	893	수	\N	\N	\N	f	\N
3616	893	월	\N	\N	\N	f	\N
3617	893	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3618	893	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3619	893	화	\N	\N	\N	f	\N
3620	897	금	\N	\N	정기휴무 (매주 금요일)	f	\N
3621	897	목	11:00:00	21:00:00	\N	f	\N
3622	897	수	11:00:00	21:00:00	\N	f	\N
3623	897	월	11:00:00	21:00:00	\N	f	\N
3624	897	일	11:00:00	17:00:00	\N	f	\N
3625	897	토	11:00:00	17:00:00	\N	f	\N
3626	897	화	11:00:00	21:00:00	\N	f	\N
3627	898	금	12:00:00	22:00:00	\N	f	\N
3628	898	목	12:00:00	22:00:00	\N	f	\N
3629	898	수	12:00:00	22:00:00	\N	f	\N
3630	898	월	12:00:00	22:00:00	\N	f	\N
3631	898	일	10:00:00	22:00:00	\N	f	\N
3632	898	토	10:00:00	22:00:00	\N	f	\N
3633	898	화	12:00:00	22:00:00	\N	f	\N
3634	899	금	09:00:00	18:30:00	\N	f	\N
3635	899	목	09:00:00	18:30:00	\N	f	\N
3636	899	수	09:00:00	18:30:00	\N	f	\N
3637	899	월	09:00:00	18:30:00	\N	f	\N
3638	899	일	\N	\N	\N	f	\N
3639	899	토	\N	\N	\N	f	\N
3640	899	화	09:00:00	18:30:00	\N	f	\N
3641	900	금	10:00:00	16:00:00	\N	f	\N
3642	900	목	10:00:00	16:00:00	\N	f	\N
3643	900	수	10:00:00	16:00:00	\N	f	\N
3644	900	월	10:00:00	16:00:00	\N	f	\N
3645	900	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3646	900	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3647	900	화	10:00:00	16:00:00	\N	f	\N
3648	877	금	11:00:00	21:30:00	\N	f	\N
3649	877	목	11:00:00	21:30:00	\N	f	\N
3650	877	수	11:00:00	21:30:00	\N	f	\N
3651	877	월	11:00:00	21:30:00	\N	f	\N
3652	877	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3653	877	토	11:00:00	21:30:00	\N	f	\N
3654	877	화	11:00:00	21:30:00	\N	f	\N
3655	878	금	11:30:00	22:00:00	\N	f	420
3656	878	목	11:30:00	22:00:00	\N	f	420
3657	878	수	11:30:00	22:00:00	\N	f	420
3658	878	월	11:30:00	22:00:00	\N	f	420
3659	878	일	\N	\N	정기휴무 (매주 일요일)	f	420
3660	878	토	11:30:00	22:00:00	\N	f	420
3661	878	화	11:30:00	22:00:00	\N	f	420
3662	879	금	11:00:00	21:00:00	\N	f	\N
3663	879	목	11:00:00	21:00:00	\N	f	\N
3664	879	수	11:00:00	21:00:00	\N	f	\N
3665	879	월	11:00:00	21:00:00	\N	f	\N
3666	879	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3667	879	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3668	879	화	11:00:00	21:00:00	\N	f	\N
3669	880	금	11:00:00	20:00:00	\N	f	60
3670	880	목	11:00:00	20:00:00	\N	f	60
3671	880	수	11:00:00	20:00:00	\N	f	60
3672	880	월	11:00:00	20:00:00	\N	f	60
3673	880	일	\N	\N	정기휴무 (매주 일요일)	f	60
3674	880	토	11:00:00	20:00:00	\N	f	60
3675	880	화	11:00:00	20:00:00	\N	f	60
3676	881	금	\N	\N	\N	f	\N
3677	881	목	\N	\N	\N	f	\N
3678	881	수	\N	\N	\N	f	\N
3679	881	월	\N	\N	\N	f	\N
3680	881	일	\N	\N	\N	f	\N
3681	881	토	\N	\N	\N	f	\N
3682	881	화	\N	\N	\N	f	\N
3683	882	금	11:00:00	19:00:00	\N	f	\N
3684	882	목	11:00:00	19:00:00	\N	f	\N
3685	882	수	11:00:00	19:00:00	\N	f	\N
3686	882	월	11:00:00	19:00:00	\N	f	\N
3687	882	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3688	882	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3689	882	화	11:00:00	19:00:00	\N	f	\N
3690	886	금	11:00:00	21:00:00	\N	f	360
3691	886	목	11:00:00	21:00:00	\N	f	360
3692	886	수	11:00:00	21:00:00	\N	f	360
3693	886	월	\N	\N	정기휴무 (매주 월요일)	f	360
4249	9	월	\N	\N	\N	f	\N
3694	886	일	11:00:00	21:00:00	\N	f	360
3695	886	토	11:00:00	21:00:00	\N	f	360
3696	886	화	11:00:00	21:00:00	\N	f	360
3697	927	금	10:30:00	20:00:00	\N	f	30
3698	927	목	10:30:00	20:00:00	\N	f	30
3699	927	수	10:30:00	20:00:00	\N	f	30
3700	927	월	10:30:00	20:00:00	\N	f	30
3701	927	일	\N	\N	정기휴무 (매주 일요일)	f	30
3702	927	토	11:30:00	19:00:00	\N	f	30
3703	927	화	10:30:00	20:00:00	\N	f	30
3704	928	금	11:30:00	20:00:00	\N	f	\N
3705	928	목	11:30:00	20:00:00	\N	f	\N
3706	928	수	11:30:00	20:00:00	\N	f	\N
3707	928	월	11:30:00	20:00:00	\N	f	\N
3708	928	일	10:30:00	19:30:00	\N	f	\N
3709	928	토	11:30:00	20:00:00	\N	f	\N
3710	928	화	11:30:00	20:00:00	\N	f	\N
3711	929	금	10:30:00	17:30:00	\N	f	\N
3712	929	목	10:30:00	17:30:00	\N	f	\N
3713	929	수	10:30:00	17:30:00	\N	f	\N
3714	929	월	10:30:00	17:30:00	\N	f	\N
3715	929	일	10:30:00	17:30:00	\N	f	\N
3716	929	토	10:30:00	17:30:00	\N	f	\N
3717	929	화	10:30:00	17:30:00	\N	f	\N
3718	930	금	10:00:00	19:00:00	\N	f	\N
3719	930	목	10:00:00	19:00:00	\N	f	\N
4123	292	월	13:00:00	\N	\N	f	\N
3720	930	수	10:00:00	19:00:00	\N	f	\N
3721	930	월	10:00:00	19:00:00	\N	f	\N
3722	930	일	\N	\N	정기휴무 (매달 1, 3, 5번째 일요일)	f	\N
3723	930	토	10:00:00	19:00:00	\N	f	\N
3724	930	화	10:00:00	19:00:00	\N	f	\N
3725	932	금	10:00:00	20:00:00	\N	f	30
3726	932	목	10:00:00	20:00:00	\N	f	30
3727	932	수	10:00:00	20:00:00	\N	f	30
3728	932	월	\N	\N	정기휴무 (매주 월요일)	f	30
3729	932	일	10:00:00	20:00:00	\N	f	30
3730	932	토	10:00:00	20:00:00	\N	f	30
3731	932	화	10:00:00	20:00:00	\N	f	30
3732	906	금	08:00:00	23:00:00	\N	f	\N
3733	906	목	08:00:00	23:00:00	\N	f	\N
3734	906	수	08:00:00	23:00:00	\N	f	\N
3735	906	월	08:00:00	23:00:00	\N	f	\N
3736	906	일	08:00:00	23:00:00	\N	f	\N
3737	906	토	08:00:00	23:00:00	\N	f	\N
3738	906	화	08:00:00	23:00:00	\N	f	\N
3739	907	금	10:30:00	20:00:00	\N	f	\N
3740	907	목	10:30:00	20:00:00	\N	f	\N
3741	907	수	10:30:00	20:00:00	\N	f	\N
3742	907	월	\N	\N	휴무	f	\N
3743	907	일	10:30:00	20:00:00	\N	f	\N
3744	907	토	10:30:00	20:00:00	\N	f	\N
3745	907	화	10:30:00	20:00:00	\N	f	\N
3746	910	금	11:00:00	21:00:00	\N	f	30
3747	910	목	11:00:00	20:00:00	\N	f	30
3748	910	수	11:00:00	20:00:00	\N	f	30
3749	910	월	11:00:00	20:00:00	\N	f	30
3750	910	일	11:00:00	21:00:00	\N	f	30
3751	910	토	11:00:00	21:00:00	\N	f	30
3752	910	화	11:00:00	20:00:00	\N	f	30
3753	912	금	10:20:00	21:30:00	\N	f	30
3754	912	목	10:20:00	21:30:00	\N	f	30
3755	912	수	10:20:00	21:30:00	\N	f	30
3756	912	월	10:20:00	19:00:00	\N	f	30
3757	912	일	10:00:00	21:00:00	\N	f	30
3758	912	토	10:20:00	21:30:00	\N	f	30
3759	912	화	10:20:00	21:30:00	\N	f	30
3760	913	금	10:00:00	18:00:00	\N	f	\N
3761	913	목	10:00:00	18:00:00	\N	f	\N
3762	913	수	10:00:00	18:00:00	\N	f	\N
3763	913	월	10:00:00	18:00:00	\N	f	\N
3764	913	일	11:00:00	19:00:00	\N	f	\N
3765	913	토	11:00:00	19:00:00	\N	f	\N
3766	913	화	10:00:00	18:00:00	\N	f	\N
3767	914	금	07:00:00	15:30:00	\N	f	\N
3768	914	목	07:00:00	15:30:00	\N	f	\N
3769	914	수	07:00:00	15:30:00	\N	f	\N
3770	914	월	07:00:00	15:30:00	\N	f	\N
3771	914	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3772	914	토	\N	\N	정기휴무 (매주 토요일)	f	\N
3773	914	화	07:00:00	15:30:00	\N	f	\N
3774	915	금	10:00:00	19:00:00	\N	f	\N
3775	915	목	10:00:00	19:00:00	\N	f	\N
3776	915	수	10:00:00	19:00:00	\N	f	\N
3777	915	월	10:00:00	19:00:00	\N	f	\N
3778	915	일	10:00:00	19:00:00	\N	f	\N
3779	915	토	10:00:00	19:00:00	\N	f	\N
3780	915	화	10:00:00	19:00:00	\N	f	\N
3781	955	금	10:30:00	21:00:00	\N	f	\N
3782	955	목	10:30:00	21:00:00	\N	f	\N
3783	955	수	10:30:00	21:00:00	\N	f	\N
3784	955	월	10:30:00	21:00:00	\N	f	\N
3785	955	일	10:30:00	21:00:00	\N	f	\N
3786	955	토	10:30:00	21:00:00	\N	f	\N
3787	955	화	10:30:00	21:00:00	\N	f	\N
3788	956	금	11:00:00	21:30:00	\N	f	390
3789	956	목	11:00:00	21:30:00	\N	f	390
3790	956	수	11:00:00	21:30:00	\N	f	390
3791	956	월	\N	\N	정기휴무 (매주 월요일)	f	390
3792	956	일	11:00:00	21:30:00	\N	f	390
3793	956	토	11:00:00	21:30:00	\N	f	390
3794	956	화	11:00:00	21:30:00	\N	f	390
3795	959	금	08:30:00	19:30:00	\N	f	\N
3796	959	목	08:30:00	19:30:00	\N	f	\N
3797	959	수	08:30:00	19:30:00	\N	f	\N
3798	959	월	08:30:00	19:30:00	\N	f	\N
3799	959	일	08:30:00	19:30:00	\N	f	\N
3800	959	토	08:30:00	19:30:00	\N	f	\N
3801	959	화	08:30:00	19:30:00	\N	f	\N
3802	961	금	08:30:00	19:00:00	\N	f	30
3803	961	목	08:30:00	19:00:00	\N	f	30
3804	961	수	08:30:00	19:00:00	\N	f	30
3805	961	월	08:30:00	19:00:00	\N	f	30
3806	961	일	09:00:00	19:00:00	\N	f	30
3807	961	토	09:00:00	19:00:00	\N	f	30
3808	961	화	08:30:00	19:00:00	\N	f	30
3809	962	금	10:00:00	22:00:00	\N	f	\N
3810	962	목	10:00:00	22:00:00	\N	f	\N
3811	962	수	10:00:00	22:00:00	\N	f	\N
3812	962	월	10:00:00	22:00:00	\N	f	\N
3813	962	일	10:00:00	22:00:00	\N	f	\N
3814	962	토	10:00:00	22:00:00	\N	f	\N
3815	962	화	10:00:00	22:00:00	\N	f	\N
3816	963	금	11:00:00	21:00:00	\N	f	360
3817	963	목	11:00:00	21:00:00	\N	f	360
3818	963	수	11:00:00	21:00:00	\N	f	360
3819	963	월	11:00:00	21:00:00	\N	f	360
3820	963	일	\N	\N	정기휴무 (매주 일요일)	f	360
3821	963	토	11:00:00	21:00:00	\N	f	360
3822	963	화	11:00:00	21:00:00	\N	f	360
3823	938	금	12:00:00	22:00:00	\N	f	\N
3824	938	목	12:00:00	22:00:00	\N	f	\N
3825	938	수	12:00:00	22:00:00	\N	f	\N
3826	938	월	12:00:00	22:00:00	\N	f	\N
3827	938	일	12:00:00	22:00:00	\N	f	\N
3828	938	토	12:00:00	22:00:00	\N	f	\N
3829	938	화	12:00:00	22:00:00	\N	f	\N
3830	940	금	09:00:00	19:00:00	\N	f	20
3831	940	목	09:00:00	19:00:00	\N	f	20
3832	940	수	09:00:00	19:00:00	\N	f	20
3833	940	월	09:00:00	19:00:00	\N	f	20
3834	940	일	10:00:00	20:00:00	\N	f	20
3835	940	토	10:00:00	20:00:00	\N	f	20
3836	940	화	09:00:00	19:00:00	\N	f	20
3837	941	금	11:00:00	19:00:00	\N	f	\N
3838	941	목	11:00:00	19:00:00	\N	f	\N
3839	941	수	11:00:00	19:00:00	\N	f	\N
3840	941	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3841	941	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3842	941	토	11:00:00	19:00:00	\N	f	\N
3843	941	화	11:00:00	19:00:00	\N	f	\N
3844	942	금	07:00:00	21:30:00	\N	f	\N
3845	942	목	07:00:00	21:30:00	\N	f	\N
3846	942	수	07:00:00	21:30:00	\N	f	\N
3847	942	월	07:00:00	21:30:00	\N	f	\N
3848	942	일	09:00:00	21:00:00	\N	f	\N
3849	942	토	09:00:00	21:00:00	\N	f	\N
3850	942	화	07:00:00	21:30:00	\N	f	\N
3851	943	금	08:30:00	17:30:00	\N	f	30
3852	943	목	08:30:00	17:30:00	\N	f	30
3853	943	수	08:30:00	17:30:00	\N	f	30
3854	943	월	08:30:00	17:30:00	\N	f	30
3855	943	일	09:00:00	20:00:00	\N	f	30
3856	943	토	09:00:00	20:00:00	\N	f	30
3857	943	화	08:30:00	17:30:00	\N	f	30
3858	944	금	07:00:00	17:00:00	\N	f	\N
3859	944	목	07:00:00	17:00:00	\N	f	\N
3860	944	수	07:00:00	17:00:00	\N	f	\N
3861	944	월	07:00:00	17:00:00	\N	f	\N
3862	944	일	08:00:00	18:00:00	\N	f	\N
3863	944	토	08:00:00	18:00:00	\N	f	\N
3864	944	화	07:00:00	17:00:00	\N	f	\N
3865	947	금	11:00:00	21:00:00	\N	f	330
3866	947	목	11:00:00	21:00:00	\N	f	330
3867	947	수	11:00:00	21:00:00	\N	f	330
3868	947	월	11:00:00	21:00:00	\N	f	330
3869	947	일	11:00:00	21:00:00	\N	f	330
3870	947	토	11:00:00	21:00:00	\N	f	330
3871	947	화	11:00:00	21:00:00	\N	f	330
3872	971	금	11:30:00	21:00:00	\N	f	380
3873	971	목	11:30:00	21:00:00	\N	f	380
3874	971	수	11:30:00	21:00:00	\N	f	380
3875	971	월	\N	\N	정기휴무 (매주 월요일)	f	380
3876	971	일	11:30:00	21:00:00	\N	f	380
3877	971	토	11:30:00	21:00:00	\N	f	380
3878	971	화	11:30:00	21:00:00	\N	f	380
3879	972	금	10:10:00	20:30:00	\N	f	\N
3880	972	목	10:10:00	20:30:00	\N	f	\N
3881	972	수	10:10:00	20:30:00	\N	f	\N
3882	972	월	10:10:00	20:30:00	\N	f	\N
3883	972	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3884	972	토	11:00:00	18:00:00	\N	f	\N
3885	972	화	10:10:00	20:30:00	\N	f	\N
3886	975	금	09:30:00	20:00:00	\N	f	\N
3887	975	목	09:30:00	20:00:00	\N	f	\N
3888	975	수	09:30:00	20:00:00	\N	f	\N
3889	975	월	09:30:00	20:00:00	\N	f	\N
3890	975	일	10:30:00	21:00:00	\N	f	\N
3891	975	토	10:30:00	21:00:00	\N	f	\N
3892	975	화	09:30:00	20:00:00	\N	f	\N
3893	977	금	09:00:00	18:00:00	\N	f	30
3894	977	목	09:00:00	18:00:00	\N	f	30
3895	977	수	09:00:00	18:00:00	\N	f	30
3896	977	월	09:00:00	18:00:00	\N	f	30
3897	977	일	10:00:00	19:00:00	\N	f	30
3898	977	토	10:00:00	19:00:00	\N	f	30
3899	977	화	09:00:00	18:00:00	\N	f	30
3900	979	금	11:00:00	20:00:00	\N	f	\N
3901	979	목	11:00:00	20:00:00	\N	f	\N
3902	979	수	11:00:00	20:00:00	\N	f	\N
3903	979	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3904	979	일	11:00:00	20:00:00	\N	f	\N
3905	979	토	11:00:00	20:00:00	\N	f	\N
3906	979	화	11:00:00	20:00:00	\N	f	\N
3907	988	금	\N	\N	휴무	f	\N
3908	988	목	\N	\N	휴무	f	\N
3909	988	수	10:00:00	18:00:00	\N	f	\N
3910	988	월	10:00:00	18:00:00	\N	f	\N
3911	988	일	\N	\N	휴무	f	\N
3912	988	토	\N	\N	휴무	f	\N
3913	988	화	10:00:00	18:00:00	\N	f	\N
3914	990	금	11:00:00	18:00:00	\N	f	\N
3915	990	목	11:00:00	18:00:00	\N	f	\N
3916	990	수	11:00:00	18:00:00	\N	f	\N
3917	990	월	11:00:00	18:00:00	\N	f	\N
3918	990	일	11:00:00	18:00:00	\N	f	\N
3919	990	토	11:00:00	18:00:00	\N	f	\N
3920	990	화	11:00:00	18:00:00	\N	f	\N
3921	992	금	10:00:00	17:00:00	\N	f	\N
3922	992	목	10:00:00	17:00:00	\N	f	\N
3923	992	수	10:00:00	17:00:00	\N	f	\N
3924	992	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3925	992	일	10:00:00	17:00:00	\N	f	\N
3926	992	토	10:00:00	17:00:00	\N	f	\N
3927	992	화	10:00:00	17:00:00	\N	f	\N
3928	994	금	11:00:00	19:00:00	\N	f	\N
3929	994	목	11:00:00	19:00:00	\N	f	\N
3930	994	수	11:00:00	19:00:00	\N	f	\N
3931	994	월	11:00:00	19:00:00	\N	f	\N
3932	994	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3933	994	토	11:00:00	17:30:00	\N	f	\N
3934	994	화	11:00:00	19:00:00	\N	f	\N
3935	997	금	\N	\N	\N	f	\N
3936	997	목	\N	\N	\N	f	\N
3937	997	수	\N	\N	\N	f	\N
3938	997	월	\N	\N	\N	f	\N
3939	997	일	\N	\N	\N	f	\N
3940	997	토	\N	\N	\N	f	\N
3941	997	화	\N	\N	\N	f	\N
3942	1018	금	14:00:00	16:30:00	\N	f	\N
3943	1018	목	14:00:00	16:30:00	\N	f	\N
3944	1018	수	14:00:00	16:30:00	\N	f	\N
3945	1018	월	14:00:00	16:30:00	\N	f	\N
3946	1018	일	14:00:00	16:30:00	\N	f	\N
3947	1018	토	14:00:00	16:30:00	\N	f	\N
3948	1018	화	14:00:00	16:30:00	\N	f	\N
3949	1019	금	11:00:00	19:00:00	\N	f	\N
3950	1019	목	11:00:00	19:00:00	\N	f	\N
3951	1019	수	11:00:00	19:00:00	\N	f	\N
3952	1019	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3953	1019	일	12:00:00	18:00:00	\N	f	\N
3954	1019	토	12:00:00	18:00:00	\N	f	\N
3955	1019	화	11:00:00	19:00:00	\N	f	\N
3956	1023	금	10:00:00	18:00:00	\N	f	\N
3957	1023	목	10:00:00	18:00:00	\N	f	\N
3958	1023	수	10:00:00	18:00:00	\N	f	\N
3959	1023	월	10:00:00	18:00:00	\N	f	\N
3960	1023	일	10:00:00	18:00:00	\N	f	\N
3961	1023	토	10:00:00	18:00:00	\N	f	\N
3962	1023	화	10:00:00	18:00:00	\N	f	\N
3963	1005	금	12:00:00	18:00:00	\N	f	\N
3964	1005	목	12:00:00	18:00:00	\N	f	\N
3965	1005	수	12:00:00	18:00:00	\N	f	\N
3966	1005	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3967	1005	일	\N	\N	정기휴무 (매주 일요일)	f	\N
3968	1005	토	12:00:00	18:00:00	\N	f	\N
3969	1005	화	12:00:00	18:00:00	\N	f	\N
3970	1006	금	\N	\N	\N	f	\N
3971	1006	목	\N	\N	\N	f	\N
3972	1006	수	\N	\N	\N	f	\N
3973	1006	월	\N	\N	\N	f	\N
3974	1006	일	\N	\N	\N	f	\N
3975	1006	토	\N	\N	\N	f	\N
3976	1006	화	\N	\N	\N	f	\N
3977	1007	금	11:00:00	17:00:00	\N	f	\N
3978	1007	목	11:00:00	17:00:00	\N	f	\N
3979	1007	수	11:00:00	17:00:00	\N	f	\N
3980	1007	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3981	1007	일	11:00:00	17:00:00	\N	f	\N
3982	1007	토	11:00:00	17:00:00	\N	f	\N
3983	1007	화	\N	\N	정기휴무 (매주 화요일)	f	\N
3984	1008	금	11:00:00	19:30:00	\N	f	\N
3985	1008	목	11:00:00	19:30:00	\N	f	\N
3986	1008	수	11:00:00	19:30:00	\N	f	\N
3987	1008	월	\N	\N	정기휴무 (매주 월요일)	f	\N
3988	1008	일	11:00:00	19:30:00	\N	f	\N
3989	1008	토	11:00:00	19:30:00	\N	f	\N
3990	1008	화	11:00:00	19:30:00	\N	f	\N
4250	9	일	\N	\N	\N	f	\N
4036	189	금	07:00:00	21:00:00	\N	f	\N
4037	189	목	07:00:00	21:00:00	\N	f	\N
4038	189	수	07:00:00	21:00:00	\N	f	\N
4039	189	월	07:00:00	21:00:00	\N	f	\N
4040	189	일	08:00:00	20:00:00	\N	f	\N
4041	189	토	08:00:00	21:00:00	\N	f	\N
4042	189	화	07:00:00	21:00:00	\N	f	\N
4043	190	금	10:00:00	21:00:00	\N	f	\N
4044	190	목	10:00:00	21:00:00	\N	f	\N
4045	190	수	10:00:00	21:00:00	\N	f	\N
4046	190	월	10:00:00	21:00:00	\N	f	\N
4047	190	일	10:00:00	21:00:00	\N	f	\N
4048	190	토	10:00:00	21:00:00	\N	f	\N
4049	190	화	10:00:00	21:00:00	\N	f	\N
4050	192	금	10:00:00	23:00:00	\N	f	\N
4051	192	목	10:00:00	23:00:00	\N	f	\N
4052	192	수	10:00:00	23:00:00	\N	f	\N
4053	192	월	10:00:00	23:00:00	\N	f	\N
4054	192	일	10:00:00	23:00:00	\N	f	\N
4055	192	토	10:00:00	21:30:00	\N	f	\N
4056	192	화	10:00:00	23:00:00	\N	f	\N
4071	230	금	11:00:00	21:00:00	\N	f	\N
4072	230	목	11:00:00	21:00:00	\N	f	\N
4073	230	수	11:00:00	21:00:00	\N	f	\N
4074	230	월	11:00:00	21:00:00	\N	f	\N
4075	230	일	11:00:00	21:00:00	\N	f	\N
4076	230	토	11:00:00	21:00:00	\N	f	\N
4077	230	화	11:00:00	21:00:00	\N	f	\N
4078	250	금	11:00:00	22:00:00	\N	f	420
4079	250	목	11:00:00	22:00:00	\N	f	420
4080	250	수	11:00:00	22:00:00	\N	f	420
4081	250	월	11:00:00	22:00:00	\N	f	420
4082	250	일	11:30:00	22:00:00	\N	f	420
4083	250	토	11:30:00	22:00:00	\N	f	420
4084	250	화	11:00:00	22:00:00	\N	f	420
4085	251	금	11:30:00	21:30:00	\N	f	390
4086	251	목	11:30:00	21:30:00	\N	f	390
4087	251	수	11:30:00	21:30:00	\N	f	390
4088	251	월	11:30:00	21:30:00	\N	f	390
4089	251	일	11:30:00	20:00:00	\N	f	390
4090	251	토	11:30:00	21:00:00	\N	f	390
4091	251	화	11:30:00	21:30:00	\N	f	390
4092	252	금	11:30:00	22:00:00	\N	f	420
4093	252	목	11:30:00	22:00:00	\N	f	420
4094	252	수	11:30:00	22:00:00	\N	f	420
4095	252	월	11:30:00	22:00:00	\N	f	420
4096	252	일	\N	\N	정기휴무 (매주 일요일)	f	420
4097	252	토	10:00:00	22:00:00	\N	f	420
4098	252	화	11:30:00	22:00:00	\N	f	420
4120	292	금	13:00:00	\N	\N	f	\N
4121	292	목	13:00:00	\N	\N	f	\N
4122	292	수	13:00:00	\N	\N	f	\N
4124	292	일	13:00:00	\N	\N	f	\N
4125	292	토	13:00:00	\N	\N	f	\N
4126	292	화	13:00:00	\N	\N	f	\N
4141	227	금	10:30:00	20:30:00	\N	f	30
4142	227	목	10:30:00	20:30:00	\N	f	30
4143	227	수	10:30:00	20:30:00	\N	f	30
4144	227	월	10:30:00	20:30:00	\N	f	30
4145	227	일	10:30:00	20:00:00	\N	f	30
4146	227	토	10:30:00	20:00:00	\N	f	30
4147	227	화	10:30:00	20:30:00	\N	f	30
4148	229	금	11:30:00	21:30:00	\N	f	390
4149	229	목	11:30:00	21:30:00	\N	f	390
4150	229	수	11:30:00	21:30:00	\N	f	390
4151	229	월	11:30:00	21:30:00	\N	f	390
4152	229	일	11:30:00	21:30:00	\N	f	390
4153	229	토	11:30:00	21:30:00	\N	f	390
4154	229	화	11:30:00	21:30:00	\N	f	390
4155	287	금	16:00:00	23:50:00	\N	f	30
4156	287	목	16:00:00	23:50:00	\N	f	30
4157	287	수	16:00:00	23:50:00	\N	f	30
4158	287	월	16:00:00	23:50:00	\N	f	30
4159	287	일	16:00:00	23:50:00	\N	f	30
4160	287	토	16:00:00	23:50:00	\N	f	30
4161	287	화	16:00:00	23:50:00	\N	f	30
4162	288	금	14:00:00	22:50:00	\N	f	90
4163	288	목	14:00:00	22:30:00	\N	f	90
4164	288	수	14:00:00	22:30:00	\N	f	90
4165	288	월	14:00:00	22:30:00	\N	f	90
4166	288	일	12:00:00	21:50:00	\N	f	90
4167	288	토	12:00:00	22:50:00	\N	f	90
4168	288	화	\N	\N	정기휴무 (매주 화요일)	f	90
4169	289	금	11:30:00	23:30:00	\N	f	570
4170	289	목	11:30:00	23:30:00	\N	f	570
4171	289	수	11:30:00	23:30:00	\N	f	570
4172	289	월	11:30:00	23:30:00	\N	f	570
4173	289	일	\N	\N	정기휴무 (매주 일요일)	f	570
4174	289	토	\N	\N	정기휴무 (매주 토요일)	f	570
4175	289	화	11:30:00	23:30:00	\N	f	570
4176	290	금	17:00:00	01:00:00	\N	f	\N
4177	290	목	17:00:00	\N	\N	f	\N
4178	290	수	17:00:00	\N	\N	f	\N
4179	290	월	17:00:00	\N	\N	f	\N
4180	290	일	15:00:00	22:30:00	\N	f	\N
4181	290	토	15:00:00	01:00:00	\N	f	\N
4182	290	화	17:00:00	\N	\N	f	\N
4183	293	금	08:30:00	20:30:00	\N	f	30
4184	293	목	08:30:00	20:30:00	\N	f	30
4185	293	수	08:30:00	20:30:00	\N	f	30
4186	293	월	08:30:00	20:30:00	\N	f	30
4187	293	일	11:00:00	19:00:00	\N	f	30
4188	293	토	\N	\N	정기휴무 (매주 토요일)	f	30
4189	293	화	08:30:00	20:30:00	\N	f	30
4190	1	금	11:00:00	21:00:00	\N	f	\N
4191	1	목	11:00:00	21:00:00	\N	f	\N
4192	1	수	11:00:00	21:00:00	\N	f	\N
4193	1	월	11:00:00	21:00:00	\N	f	\N
4194	1	일	11:00:00	21:00:00	\N	f	\N
4195	1	토	11:00:00	21:00:00	\N	f	\N
4196	1	화	11:00:00	21:00:00	\N	f	\N
4197	2	금	08:00:00	22:00:00	\N	f	30
4198	2	목	08:00:00	22:00:00	\N	f	30
4199	2	수	08:00:00	22:00:00	\N	f	30
4200	2	월	08:00:00	22:00:00	\N	f	30
4201	2	일	08:00:00	22:00:00	\N	f	30
4202	2	토	08:00:00	22:00:00	\N	f	30
4203	2	화	08:00:00	22:00:00	\N	f	30
4204	3	금	12:00:00	21:00:00	\N	f	\N
4205	3	목	12:00:00	21:00:00	\N	f	\N
4206	3	수	12:00:00	21:00:00	\N	f	\N
4207	3	월	12:00:00	21:00:00	\N	f	\N
4208	3	일	12:00:00	21:00:00	\N	f	\N
4209	3	토	12:00:00	21:00:00	\N	f	\N
4210	3	화	12:00:00	21:00:00	\N	f	\N
4211	4	금	11:00:00	19:00:00	\N	f	\N
4212	4	목	11:00:00	19:00:00	\N	f	\N
4213	4	수	11:00:00	19:00:00	\N	f	\N
4214	4	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4215	4	일	11:00:00	19:00:00	\N	f	\N
4216	4	토	11:00:00	19:00:00	\N	f	\N
4217	4	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4218	5	금	12:00:00	21:00:00	\N	f	\N
4219	5	목	12:00:00	19:00:00	\N	f	\N
4220	5	수	12:00:00	19:00:00	\N	f	\N
4221	5	월	12:00:00	19:00:00	\N	f	\N
4222	5	일	12:00:00	21:00:00	\N	f	\N
4223	5	토	12:00:00	21:00:00	\N	f	\N
4224	5	화	12:00:00	19:00:00	\N	f	\N
4225	6	금	11:00:00	18:00:00	\N	f	\N
4226	6	목	11:00:00	18:00:00	\N	f	\N
4227	6	수	11:00:00	18:00:00	\N	f	\N
4228	6	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4229	6	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4230	6	토	11:00:00	18:00:00	\N	f	\N
4231	6	화	11:00:00	18:00:00	\N	f	\N
4232	7	금	08:00:00	21:00:00	\N	f	\N
4233	7	목	08:00:00	21:00:00	\N	f	\N
4234	7	수	08:00:00	21:00:00	\N	f	\N
4235	7	월	08:00:00	21:00:00	\N	f	\N
4236	7	일	08:00:00	21:00:00	\N	f	\N
4237	7	토	08:00:00	21:00:00	\N	f	\N
4238	7	화	08:00:00	21:00:00	\N	f	\N
4239	8	금	10:00:00	18:00:00	\N	f	\N
4240	8	목	10:00:00	18:00:00	\N	f	\N
4241	8	수	10:00:00	18:00:00	\N	f	\N
4242	8	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4243	8	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4244	8	토	10:00:00	18:00:00	\N	f	\N
4245	8	화	10:00:00	18:00:00	\N	f	\N
4246	9	금	\N	\N	\N	f	\N
4247	9	목	\N	\N	\N	f	\N
4251	9	토	\N	\N	\N	f	\N
4252	9	화	\N	\N	\N	f	\N
4253	20	금	09:30:00	19:00:00	\N	f	30
4254	20	목	09:30:00	19:00:00	\N	f	30
4255	20	수	09:30:00	19:00:00	\N	f	30
4256	20	월	09:30:00	19:00:00	\N	f	30
4257	20	일	09:30:00	19:00:00	\N	f	30
4258	20	토	09:30:00	19:00:00	\N	f	30
4259	20	화	09:30:00	19:00:00	\N	f	30
4260	21	금	10:30:00	21:00:00	\N	f	\N
4261	21	목	10:30:00	21:00:00	\N	f	\N
4262	21	수	10:30:00	21:00:00	\N	f	\N
4263	21	월	11:00:00	21:00:00	\N	f	\N
4264	21	일	10:30:00	21:00:00	\N	f	\N
4265	21	토	10:30:00	21:00:00	\N	f	\N
4266	21	화	10:30:00	21:00:00	\N	f	\N
4267	22	금	\N	\N	휴무	f	\N
4268	22	목	\N	\N	휴무	f	\N
4269	22	수	\N	\N	휴무	f	\N
4270	22	월	\N	\N	휴무	f	\N
4271	22	일	\N	\N	휴무	f	\N
4272	22	토	\N	\N	휴무	f	\N
4273	22	화	\N	\N	휴무	f	\N
4274	23	금	\N	\N	\N	f	\N
4275	23	목	\N	\N	\N	f	\N
4276	23	수	\N	\N	\N	f	\N
4277	23	월	\N	\N	\N	f	\N
4278	23	일	\N	\N	\N	f	\N
4279	23	토	\N	\N	\N	f	\N
4280	23	화	\N	\N	\N	f	\N
4281	24	금	12:00:00	21:00:00	\N	f	\N
4282	24	목	12:00:00	21:00:00	\N	f	\N
4283	24	수	12:00:00	21:00:00	\N	f	\N
4284	24	월	12:00:00	21:00:00	\N	f	\N
4285	24	일	12:00:00	21:00:00	\N	f	\N
4286	24	토	12:00:00	21:00:00	\N	f	\N
4287	24	화	12:00:00	21:00:00	\N	f	\N
4288	25	금	08:30:00	17:00:00	\N	f	30
4289	25	목	08:30:00	17:00:00	\N	f	30
4290	25	수	08:30:00	17:00:00	\N	f	30
4291	25	월	08:30:00	17:00:00	\N	f	30
4292	25	일	10:00:00	18:00:00	\N	f	30
4293	25	토	10:00:00	18:00:00	\N	f	30
4294	25	화	08:30:00	17:00:00	\N	f	30
4295	26	금	10:00:00	16:40:00	\N	f	\N
4296	26	목	10:00:00	16:40:00	\N	f	\N
4297	26	수	10:00:00	16:40:00	\N	f	\N
4298	26	월	10:00:00	16:40:00	\N	f	\N
4299	26	일	10:00:00	16:40:00	\N	f	\N
4300	26	토	10:00:00	16:40:00	\N	f	\N
4301	26	화	10:00:00	16:40:00	\N	f	\N
4302	27	금	13:00:00	18:00:00	\N	f	\N
4303	27	목	13:00:00	18:00:00	\N	f	\N
4304	27	수	13:00:00	18:00:00	\N	f	\N
4305	27	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4306	27	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4307	27	토	13:00:00	18:00:00	\N	f	\N
4308	27	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4309	28	금	\N	\N	\N	f	\N
4310	28	목	\N	\N	\N	f	\N
4311	28	수	\N	\N	\N	f	\N
4312	28	월	\N	\N	\N	f	\N
4313	28	일	\N	\N	\N	f	\N
4314	28	토	\N	\N	\N	f	\N
4315	28	화	\N	\N	\N	f	\N
4316	29	금	12:00:00	20:00:00	\N	f	60
4317	29	목	12:00:00	20:00:00	\N	f	60
4318	29	수	12:00:00	20:00:00	\N	f	60
4319	29	월	\N	\N	정기휴무 (매주 월요일)	f	60
4320	29	일	12:00:00	19:00:00	\N	f	60
4321	29	토	12:00:00	20:00:00	\N	f	60
4322	29	화	12:00:00	18:00:00	\N	f	60
4323	40	금	10:00:00	22:00:00	\N	f	30
4324	40	목	11:00:00	22:00:00	\N	f	30
4325	40	수	\N	\N	휴무	f	30
4326	40	월	11:00:00	22:00:00	\N	f	30
4327	40	일	10:00:00	22:00:00	\N	f	30
4328	40	토	10:00:00	22:00:00	\N	f	30
4329	40	화	\N	\N	휴무	f	30
4330	41	금	11:00:00	20:30:00	\N	f	\N
4331	41	목	11:00:00	19:30:00	\N	f	\N
4332	41	수	11:00:00	19:30:00	\N	f	\N
4333	41	월	11:00:00	19:30:00	\N	f	\N
4334	41	일	11:00:00	19:30:00	\N	f	\N
4335	41	토	11:00:00	20:30:00	\N	f	\N
4336	41	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4337	42	금	11:00:00	21:00:00	\N	f	\N
4338	42	목	11:00:00	21:00:00	\N	f	\N
4339	42	수	11:00:00	21:00:00	\N	f	\N
4340	42	월	11:00:00	21:00:00	\N	f	\N
4341	42	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4342	42	토	11:00:00	21:00:00	\N	f	\N
4343	42	화	11:00:00	21:00:00	\N	f	\N
4344	43	금	12:15:00	22:00:00	\N	f	\N
4345	43	목	12:15:00	22:00:00	\N	f	\N
4346	43	수	12:15:00	22:00:00	\N	f	\N
4347	43	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4348	43	일	12:30:00	21:00:00	\N	f	\N
4349	43	토	12:00:00	22:00:00	\N	f	\N
4350	43	화	12:15:00	22:00:00	\N	f	\N
4351	44	금	10:00:00	18:00:00	\N	f	10
4352	44	목	10:00:00	18:00:00	\N	f	10
4353	44	수	10:00:00	18:00:00	\N	f	10
4354	44	월	10:00:00	18:00:00	\N	f	10
4355	44	일	10:00:00	17:00:00	\N	f	10
4356	44	토	10:00:00	18:00:00	\N	f	10
4357	44	화	10:00:00	18:00:00	\N	f	10
4358	45	금	09:00:00	20:00:00	\N	f	30
4359	45	목	09:00:00	20:00:00	\N	f	30
4360	45	수	09:00:00	20:00:00	\N	f	30
4361	45	월	09:00:00	20:00:00	\N	f	30
4362	45	일	11:00:00	19:00:00	\N	f	30
4363	45	토	11:00:00	19:00:00	\N	f	30
4364	45	화	09:00:00	20:00:00	\N	f	30
4365	46	금	11:00:00	21:00:00	\N	f	375
4366	46	목	11:00:00	21:00:00	\N	f	375
4367	46	수	11:00:00	21:00:00	\N	f	375
4368	46	월	11:00:00	21:00:00	\N	f	375
4369	46	일	11:00:00	21:00:00	\N	f	375
4370	46	토	11:00:00	21:00:00	\N	f	375
4371	46	화	\N	\N	정기휴무 (매주 화요일)	f	375
4372	47	금	11:00:00	21:30:00	\N	f	390
4373	47	목	11:00:00	21:30:00	\N	f	390
4374	47	수	11:00:00	21:30:00	\N	f	390
4375	47	월	11:00:00	21:30:00	\N	f	390
4376	47	일	11:00:00	21:00:00	\N	f	390
4377	47	토	11:00:00	21:30:00	\N	f	390
4378	47	화	11:00:00	21:30:00	\N	f	390
4379	48	금	08:30:00	20:00:00	\N	f	\N
4380	48	목	08:30:00	20:00:00	\N	f	\N
4381	48	수	08:30:00	20:00:00	\N	f	\N
4382	48	월	08:30:00	20:00:00	\N	f	\N
4383	48	일	08:30:00	20:00:00	\N	f	\N
4384	48	토	08:30:00	20:00:00	\N	f	\N
4385	48	화	08:30:00	20:00:00	\N	f	\N
4386	59	금	\N	\N	\N	f	\N
4387	59	목	\N	\N	\N	f	\N
4388	59	수	\N	\N	\N	f	\N
4389	59	월	\N	\N	\N	f	\N
4390	59	일	\N	\N	\N	f	\N
4391	59	토	\N	\N	\N	f	\N
4392	59	화	\N	\N	\N	f	\N
4393	60	금	12:00:00	21:00:00	\N	f	\N
4394	60	목	12:00:00	21:00:00	\N	f	\N
4395	60	수	12:00:00	21:00:00	\N	f	\N
4396	60	월	12:00:00	21:00:00	\N	f	\N
4397	60	일	12:00:00	21:00:00	\N	f	\N
4398	60	토	12:00:00	21:00:00	\N	f	\N
4399	60	화	12:00:00	21:00:00	\N	f	\N
4400	61	금	10:00:00	22:00:00	\N	f	90
4401	61	목	10:00:00	22:00:00	\N	f	90
4402	61	수	10:00:00	22:00:00	\N	f	90
4403	61	월	\N	\N	정기휴무 (매주 월요일)	f	90
4404	61	일	10:00:00	21:00:00	\N	f	90
4405	61	토	10:00:00	21:00:00	\N	f	90
4406	61	화	10:00:00	22:00:00	\N	f	90
4407	62	금	11:30:00	17:00:00	\N	f	30
4408	62	목	11:30:00	17:00:00	\N	f	30
4409	62	수	11:30:00	17:00:00	\N	f	30
4410	62	월	\N	\N	정기휴무 (매주 월요일)	f	30
4411	62	일	11:30:00	20:30:00	\N	f	30
4412	62	토	11:30:00	20:30:00	\N	f	30
4413	62	화	\N	\N	정기휴무 (매주 화요일)	f	30
4414	63	금	11:00:00	16:00:00	\N	f	60
4415	63	목	11:00:00	16:00:00	\N	f	60
4416	63	수	11:00:00	16:00:00	\N	f	60
4417	63	월	11:00:00	16:00:00	\N	f	60
4418	63	일	11:00:00	17:00:00	\N	f	60
4419	63	토	11:00:00	17:00:00	\N	f	60
4420	63	화	\N	\N	정기휴무 (매주 화요일)	f	60
4421	64	금	11:00:00	21:00:00	\N	f	360
4422	64	목	11:00:00	21:00:00	\N	f	360
4423	64	수	11:00:00	21:00:00	\N	f	360
4424	64	월	11:00:00	21:00:00	\N	f	360
4425	64	일	11:00:00	20:30:00	\N	f	360
4426	64	토	11:00:00	21:00:00	\N	f	360
4427	64	화	11:00:00	21:00:00	\N	f	360
4428	65	금	11:30:00	21:05:00	\N	f	365
4429	65	목	11:30:00	21:05:00	\N	f	365
4430	65	수	11:30:00	21:05:00	\N	f	365
4431	65	월	11:30:00	21:05:00	\N	f	365
4432	65	일	\N	\N	정기휴무 (매주 일요일)	f	365
4433	65	토	11:30:00	20:00:00	\N	f	365
4434	65	화	11:30:00	21:05:00	\N	f	365
4435	66	금	11:30:00	22:00:00	\N	f	420
4436	66	목	11:30:00	22:00:00	\N	f	420
4437	66	수	11:30:00	22:00:00	\N	f	420
4438	66	월	\N	\N	정기휴무 (매주 월요일)	f	420
4439	66	일	12:30:00	20:00:00	\N	f	420
4440	66	토	11:30:00	22:00:00	\N	f	420
4441	66	화	11:30:00	22:00:00	\N	f	420
4442	67	금	07:00:00	20:30:00	\N	f	\N
4443	67	목	07:00:00	20:30:00	\N	f	\N
4444	67	수	07:00:00	20:30:00	\N	f	\N
4445	67	월	07:00:00	20:30:00	\N	f	\N
4446	67	일	07:00:00	20:30:00	\N	f	\N
4447	67	토	07:00:00	20:30:00	\N	f	\N
4448	67	화	07:00:00	20:30:00	\N	f	\N
4449	68	금	11:30:00	21:00:00	\N	f	60
4450	68	목	11:30:00	21:00:00	\N	f	60
4451	68	수	11:30:00	21:00:00	\N	f	60
4452	68	월	11:30:00	21:00:00	\N	f	60
4453	68	일	11:30:00	21:00:00	\N	f	60
4454	68	토	11:30:00	21:00:00	\N	f	60
4455	68	화	11:30:00	21:00:00	\N	f	60
4456	79	금	11:30:00	21:30:00	\N	f	390
4457	79	목	11:30:00	21:30:00	\N	f	390
4458	79	수	11:30:00	21:30:00	\N	f	390
4459	79	월	11:30:00	21:30:00	\N	f	390
4460	79	일	\N	\N	정기휴무 (매주 일요일)	f	390
4461	79	토	11:30:00	21:30:00	\N	f	390
4462	79	화	11:30:00	21:30:00	\N	f	390
4463	80	금	11:30:00	22:00:00	\N	f	\N
4464	80	목	11:30:00	22:00:00	\N	f	\N
4465	80	수	11:30:00	22:00:00	\N	f	\N
4466	80	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4467	80	일	11:30:00	22:00:00	\N	f	\N
4468	80	토	11:30:00	22:00:00	\N	f	\N
4469	80	화	11:30:00	22:00:00	\N	f	\N
4470	82	금	11:00:00	21:30:00	\N	f	\N
4577	106	수	\N	\N	\N	f	\N
4471	82	목	11:00:00	21:30:00	\N	f	\N
4472	82	수	11:00:00	21:30:00	\N	f	\N
4473	82	월	11:00:00	21:30:00	\N	f	\N
4474	82	일	11:00:00	21:30:00	\N	f	\N
4475	82	토	11:00:00	21:30:00	\N	f	\N
4476	82	화	11:00:00	21:30:00	\N	f	\N
4477	83	금	11:30:00	23:00:00	\N	f	480
4478	83	목	11:30:00	23:00:00	\N	f	480
4479	83	수	11:30:00	23:00:00	\N	f	480
4480	83	월	\N	\N	정기휴무 (매주 월요일)	f	480
4481	83	일	11:30:00	23:00:00	\N	f	480
4482	83	토	11:30:00	23:00:00	\N	f	480
4483	83	화	10:00:00	22:00:00	\N	f	480
4484	84	금	12:00:00	21:00:00	\N	f	330
4485	84	목	12:00:00	21:00:00	\N	f	330
4486	84	수	12:00:00	21:00:00	\N	f	330
4487	84	월	\N	\N	정기휴무 (매주 월요일)	f	330
4488	84	일	12:00:00	21:00:00	\N	f	330
4489	84	토	12:00:00	21:00:00	\N	f	330
4490	84	화	\N	\N	정기휴무 (매주 화요일)	f	330
4491	85	금	11:30:00	20:50:00	\N	f	360
4492	85	목	11:30:00	20:50:00	\N	f	360
4493	85	수	11:30:00	20:50:00	\N	f	360
4494	85	월	\N	\N	정기휴무 (매주 월요일)	f	360
4495	85	일	11:30:00	20:50:00	\N	f	360
4496	85	토	11:30:00	20:50:00	\N	f	360
4497	85	화	11:30:00	20:50:00	\N	f	360
4498	86	금	12:00:00	20:00:00	\N	f	\N
4499	86	목	12:00:00	20:00:00	\N	f	\N
4500	86	수	12:00:00	20:00:00	\N	f	\N
4501	86	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4502	86	일	12:00:00	20:00:00	\N	f	\N
4503	86	토	12:00:00	20:00:00	\N	f	\N
4504	86	화	12:00:00	20:00:00	\N	f	\N
4505	87	금	11:00:00	22:00:00	\N	f	60
4506	87	목	11:00:00	22:00:00	\N	f	60
4507	87	수	11:00:00	22:00:00	\N	f	60
4508	87	월	11:00:00	22:00:00	\N	f	60
4509	87	일	11:00:00	22:00:00	\N	f	60
4510	87	토	11:00:00	22:00:00	\N	f	60
4511	87	화	\N	\N	정기휴무 (매주 화요일)	f	60
4512	88	금	11:00:00	22:00:00	\N	f	\N
4513	88	목	11:00:00	22:00:00	\N	f	\N
4514	88	수	11:00:00	22:00:00	\N	f	\N
4515	88	월	11:00:00	22:00:00	\N	f	\N
4516	88	일	11:00:00	22:00:00	\N	f	\N
4517	88	토	11:00:00	22:00:00	\N	f	\N
4518	88	화	11:00:00	22:00:00	\N	f	\N
4519	98	금	11:30:00	21:30:00	\N	f	\N
4520	98	목	11:30:00	21:30:00	\N	f	\N
4521	98	수	11:30:00	21:30:00	\N	f	\N
4522	98	월	11:30:00	21:30:00	\N	f	\N
4523	98	일	11:30:00	21:00:00	\N	f	\N
4524	98	토	11:30:00	21:00:00	\N	f	\N
4525	98	화	11:30:00	21:30:00	\N	f	\N
4526	99	금	11:30:00	21:30:00	\N	f	\N
4527	99	목	11:30:00	21:30:00	\N	f	\N
4528	99	수	11:30:00	21:30:00	\N	f	\N
4529	99	월	11:30:00	21:30:00	\N	f	\N
4530	99	일	11:30:00	21:30:00	\N	f	\N
4531	99	토	11:30:00	21:30:00	\N	f	\N
4532	99	화	11:30:00	21:30:00	\N	f	\N
4533	100	금	10:00:00	19:00:00	\N	f	\N
4534	100	목	10:00:00	19:00:00	\N	f	\N
4535	100	수	10:00:00	19:00:00	\N	f	\N
4536	100	월	10:00:00	19:00:00	\N	f	\N
4537	100	일	11:00:00	19:00:00	\N	f	\N
4538	100	토	10:00:00	19:00:00	\N	f	\N
4539	100	화	10:00:00	19:00:00	\N	f	\N
4540	101	금	12:00:00	22:00:00	\N	f	420
4541	101	목	12:00:00	22:00:00	\N	f	420
4542	101	수	12:00:00	22:00:00	\N	f	420
4543	101	월	\N	\N	정기휴무 (매주 월요일)	f	420
4544	101	일	\N	\N	정기휴무 (매주 일요일)	f	420
4545	101	토	12:30:00	22:00:00	\N	f	420
4546	101	화	12:00:00	22:00:00	\N	f	420
4547	102	금	11:30:00	21:00:00	\N	f	360
4548	102	목	11:30:00	21:00:00	\N	f	360
4549	102	수	11:30:00	21:00:00	\N	f	360
4550	102	월	\N	\N	정기휴무 (매주 월요일)	f	360
4551	102	일	11:30:00	21:00:00	\N	f	360
4552	102	토	11:30:00	21:00:00	\N	f	360
4553	102	화	11:30:00	21:00:00	\N	f	360
4554	103	금	11:00:00	23:30:00	\N	f	450
4555	103	목	11:00:00	23:30:00	\N	f	450
4556	103	수	11:00:00	23:30:00	\N	f	450
4557	103	월	\N	\N	정기휴무 (매주 월요일)	f	450
4558	103	일	11:00:00	23:30:00	\N	f	450
4559	103	토	11:00:00	23:30:00	\N	f	450
4560	103	화	11:00:00	23:30:00	\N	f	450
4561	104	금	11:30:00	21:30:00	\N	f	390
4562	104	목	11:30:00	21:30:00	\N	f	390
4563	104	수	11:30:00	21:30:00	\N	f	390
4564	104	월	11:30:00	21:30:00	\N	f	390
4565	104	일	11:30:00	21:30:00	\N	f	390
4566	104	토	11:30:00	21:30:00	\N	f	390
4567	104	화	11:30:00	21:30:00	\N	f	390
4568	105	금	11:30:00	21:30:00	\N	f	390
4569	105	목	11:30:00	21:30:00	\N	f	390
4570	105	수	11:30:00	21:30:00	\N	f	390
4571	105	월	11:30:00	21:30:00	\N	f	390
4572	105	일	11:30:00	21:30:00	\N	f	390
4573	105	토	11:30:00	21:30:00	\N	f	390
4574	105	화	11:30:00	21:30:00	\N	f	390
4575	106	금	\N	\N	\N	f	\N
4576	106	목	\N	\N	\N	f	\N
4578	106	월	\N	\N	\N	f	\N
4579	106	일	\N	\N	\N	f	\N
4580	106	토	\N	\N	\N	f	\N
4581	106	화	\N	\N	\N	f	\N
4582	107	금	17:00:00	01:00:00	\N	f	\N
4583	107	목	17:00:00	\N	\N	f	\N
4584	107	수	17:00:00	\N	\N	f	\N
4585	107	월	17:00:00	\N	\N	f	\N
4586	107	일	14:00:00	\N	\N	f	\N
4587	107	토	14:00:00	01:00:00	\N	f	\N
4588	107	화	17:00:00	\N	\N	f	\N
4589	118	금	11:00:00	20:00:00	\N	f	\N
4590	118	목	11:00:00	20:00:00	\N	f	\N
4591	118	수	11:00:00	20:00:00	\N	f	\N
4592	118	월	11:00:00	20:00:00	\N	f	\N
4593	118	일	11:00:00	21:00:00	\N	f	\N
4594	118	토	11:00:00	21:00:00	\N	f	\N
4595	118	화	11:00:00	20:00:00	\N	f	\N
4596	119	금	17:30:00	\N	\N	f	\N
4597	119	목	17:30:00	\N	\N	f	\N
4598	119	수	17:30:00	\N	\N	f	\N
4599	119	월	17:30:00	\N	\N	f	\N
4600	119	일	17:30:00	\N	\N	f	\N
4601	119	토	17:30:00	\N	\N	f	\N
4602	119	화	17:30:00	\N	\N	f	\N
4603	120	금	16:00:00	22:00:00	\N	f	120
4604	120	목	16:00:00	22:00:00	\N	f	120
4605	120	수	16:00:00	22:00:00	\N	f	120
4606	120	월	16:00:00	22:00:00	\N	f	120
4607	120	일	\N	\N	정기휴무 (매주 일요일)	f	120
4608	120	토	16:00:00	22:00:00	\N	f	120
4609	120	화	16:00:00	22:00:00	\N	f	120
4610	121	금	09:00:00	21:00:00	\N	f	\N
4611	121	목	09:00:00	21:00:00	\N	f	\N
4612	121	수	09:00:00	21:00:00	\N	f	\N
4613	121	월	09:00:00	21:00:00	\N	f	\N
4614	121	일	09:00:00	21:00:00	\N	f	\N
4615	121	토	09:00:00	21:00:00	\N	f	\N
4616	121	화	09:00:00	21:00:00	\N	f	\N
4617	122	금	08:00:00	16:00:00	\N	f	30
4618	122	목	08:00:00	16:00:00	\N	f	30
4619	122	수	08:00:00	16:00:00	\N	f	30
4620	122	월	08:00:00	16:00:00	\N	f	30
4621	122	일	\N	\N	정기휴무 (매주 일요일)	f	30
4622	122	토	11:00:00	16:00:00	\N	f	30
4623	122	화	08:00:00	16:00:00	\N	f	30
4624	123	금	19:00:00	\N	\N	f	30
4625	123	목	19:00:00	\N	\N	f	30
4626	123	수	19:00:00	\N	\N	f	30
4627	123	월	19:00:00	\N	\N	f	30
4628	123	일	\N	\N	정기휴무 (매주 일요일)	f	30
4629	123	토	19:00:00	\N	\N	f	30
4630	123	화	19:00:00	\N	\N	f	30
4631	126	금	08:30:00	19:00:00	\N	f	\N
4632	126	목	08:30:00	19:00:00	\N	f	\N
4633	126	수	08:30:00	19:00:00	\N	f	\N
4634	126	월	08:30:00	19:00:00	\N	f	\N
4635	126	일	08:30:00	19:00:00	\N	f	\N
4636	126	토	08:30:00	19:00:00	\N	f	\N
4637	126	화	08:30:00	19:00:00	\N	f	\N
4638	127	금	18:00:00	01:30:00	\N	f	30
4639	127	목	18:00:00	01:30:00	\N	f	30
4640	127	수	18:00:00	01:30:00	\N	f	30
4641	127	월	18:00:00	01:30:00	\N	f	30
4642	127	일	\N	\N	정기휴무 (매주 일요일)	f	30
4643	127	토	18:00:00	01:30:00	\N	f	30
4644	127	화	18:00:00	01:30:00	\N	f	30
4645	136	금	12:00:00	19:00:00	\N	f	\N
4646	136	목	12:00:00	19:00:00	\N	f	\N
4647	136	수	12:00:00	19:00:00	\N	f	\N
4648	136	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4649	136	일	12:00:00	19:00:00	\N	f	\N
4650	136	토	12:00:00	19:00:00	\N	f	\N
4651	136	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4652	137	금	10:00:00	18:00:00	\N	f	\N
4653	137	목	10:00:00	18:00:00	\N	f	\N
4654	137	수	10:00:00	18:00:00	\N	f	\N
4655	137	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4656	137	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4657	137	토	10:00:00	18:00:00	\N	f	\N
4658	137	화	10:00:00	18:00:00	\N	f	\N
4659	138	금	11:00:00	19:00:00	\N	f	\N
4660	138	목	\N	\N	정기휴무 (매주 목요일)	f	\N
4661	138	수	\N	\N	정기휴무 (매주 수요일)	f	\N
4662	138	월	11:00:00	19:00:00	\N	f	\N
4663	138	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4664	138	토	11:00:00	17:00:00	\N	f	\N
4665	138	화	11:00:00	19:00:00	\N	f	\N
4666	139	금	12:00:00	19:00:00	\N	f	\N
4667	139	목	12:00:00	19:00:00	\N	f	\N
4668	139	수	12:00:00	19:00:00	\N	f	\N
4669	139	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4670	139	일	12:00:00	19:00:00	\N	f	\N
4671	139	토	12:00:00	19:00:00	\N	f	\N
4672	139	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4673	140	금	10:00:00	18:00:00	\N	f	\N
4674	140	목	10:00:00	18:00:00	\N	f	\N
4675	140	수	10:00:00	18:00:00	\N	f	\N
4676	140	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4677	140	일	10:00:00	18:00:00	\N	f	\N
4678	140	토	10:00:00	18:00:00	\N	f	\N
4679	140	화	10:00:00	18:00:00	\N	f	\N
4680	141	금	11:00:00	18:00:00	\N	f	\N
4681	141	목	11:00:00	18:00:00	\N	f	\N
4682	141	수	11:00:00	18:00:00	\N	f	\N
4683	141	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4684	141	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4685	141	토	11:00:00	18:00:00	\N	f	\N
4686	141	화	11:00:00	18:00:00	\N	f	\N
4687	143	금	13:00:00	22:00:00	\N	f	\N
4688	143	목	13:00:00	22:00:00	\N	f	\N
4689	143	수	13:00:00	22:00:00	\N	f	\N
4690	143	월	13:00:00	22:00:00	\N	f	\N
4691	143	일	13:00:00	22:00:00	\N	f	\N
4692	143	토	13:00:00	22:00:00	\N	f	\N
4693	143	화	13:00:00	22:00:00	\N	f	\N
4694	144	금	11:00:00	18:00:00	\N	f	\N
4695	144	목	11:00:00	18:00:00	\N	f	\N
4696	144	수	11:00:00	18:00:00	\N	f	\N
4697	144	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4698	144	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4699	144	토	11:00:00	18:00:00	\N	f	\N
4700	144	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4701	145	금	13:00:00	19:00:00	\N	f	\N
4702	145	목	13:00:00	19:00:00	\N	f	\N
4703	145	수	13:00:00	19:00:00	\N	f	\N
4704	145	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4705	145	일	13:00:00	19:00:00	\N	f	\N
4706	145	토	13:00:00	19:00:00	\N	f	\N
4707	145	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4708	155	금	\N	\N	\N	f	\N
4709	155	목	\N	\N	\N	f	\N
4710	155	수	\N	\N	\N	f	\N
4711	155	월	\N	\N	\N	f	\N
4712	155	일	\N	\N	\N	f	\N
4713	155	토	\N	\N	\N	f	\N
4714	155	화	\N	\N	\N	f	\N
4715	156	금	10:00:00	18:00:00	\N	f	\N
4716	156	목	10:00:00	18:00:00	\N	f	\N
4717	156	수	10:00:00	18:00:00	\N	f	\N
4718	156	월	10:00:00	18:00:00	\N	f	\N
4719	156	일	\N	\N	\N	f	\N
4720	156	토	\N	\N	\N	f	\N
4721	156	화	10:00:00	18:00:00	\N	f	\N
4722	157	금	12:00:00	18:00:00	\N	f	\N
4723	157	목	12:00:00	18:00:00	\N	f	\N
4724	157	수	12:00:00	18:00:00	\N	f	\N
4725	157	월	12:00:00	18:00:00	\N	f	\N
4726	157	일	12:00:00	18:00:00	\N	f	\N
4727	157	토	12:00:00	18:00:00	\N	f	\N
4728	157	화	12:00:00	18:00:00	\N	f	\N
4729	158	금	\N	\N	정기휴무 (매주 금요일)	f	\N
4730	158	목	\N	\N	정기휴무 (매주 목요일)	f	\N
4731	158	수	\N	\N	정기휴무 (매주 수요일)	f	\N
4732	158	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4733	158	일	10:00:00	17:00:00	\N	f	\N
4734	158	토	10:00:00	17:00:00	\N	f	\N
4735	158	화	\N	\N	정기휴무 (매주 화요일)	f	\N
4736	159	금	10:00:00	21:00:00	\N	f	\N
4737	159	목	10:00:00	21:00:00	\N	f	\N
4738	159	수	10:00:00	21:00:00	\N	f	\N
4739	159	월	10:00:00	21:00:00	\N	f	\N
4740	159	일	10:00:00	21:00:00	\N	f	\N
4741	159	토	10:00:00	21:00:00	\N	f	\N
4742	159	화	10:00:00	21:00:00	\N	f	\N
4743	161	금	11:00:00	19:00:00	\N	f	\N
4744	161	목	11:00:00	19:00:00	\N	f	\N
4745	161	수	11:00:00	19:00:00	\N	f	\N
4746	161	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4747	161	일	11:00:00	19:00:00	\N	f	\N
4748	161	토	11:00:00	19:00:00	\N	f	\N
4749	161	화	11:00:00	19:00:00	\N	f	\N
4750	162	금	11:00:00	19:30:00	\N	f	\N
4751	162	목	11:00:00	19:30:00	\N	f	\N
4752	162	수	11:00:00	19:30:00	\N	f	\N
4753	162	월	11:00:00	19:30:00	\N	f	\N
4754	162	일	11:00:00	20:00:00	\N	f	\N
4755	162	토	11:00:00	20:00:00	\N	f	\N
4756	162	화	11:00:00	19:30:00	\N	f	\N
4757	173	금	08:00:00	18:00:00	\N	f	60
4758	173	목	08:00:00	18:00:00	\N	f	60
4759	173	수	08:00:00	18:00:00	\N	f	60
4760	173	월	08:00:00	18:00:00	\N	f	60
4761	173	일	09:00:00	18:00:00	\N	f	60
4762	173	토	09:00:00	18:00:00	\N	f	60
4763	173	화	08:00:00	18:00:00	\N	f	60
4764	174	금	12:00:00	21:00:00	\N	f	\N
4765	174	목	12:00:00	21:00:00	\N	f	\N
4766	174	수	12:00:00	21:00:00	\N	f	\N
4767	174	월	12:00:00	21:00:00	\N	f	\N
4768	174	일	12:00:00	21:00:00	\N	f	\N
4769	174	토	12:00:00	21:00:00	\N	f	\N
4770	174	화	12:00:00	21:00:00	\N	f	\N
4771	175	금	11:00:00	22:00:00	\N	f	\N
4772	175	목	11:00:00	22:00:00	\N	f	\N
4773	175	수	11:00:00	22:00:00	\N	f	\N
4774	175	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4775	175	일	11:00:00	21:00:00	\N	f	\N
4776	175	토	11:00:00	22:00:00	\N	f	\N
4777	175	화	11:00:00	22:00:00	\N	f	\N
4778	176	금	08:00:00	21:00:00	\N	f	\N
4779	176	목	08:00:00	21:00:00	\N	f	\N
4780	176	수	08:00:00	21:00:00	\N	f	\N
4781	176	월	08:00:00	21:00:00	\N	f	\N
4782	176	일	08:00:00	21:00:00	\N	f	\N
4783	176	토	08:00:00	21:00:00	\N	f	\N
4784	176	화	08:00:00	20:00:00	\N	f	\N
4785	177	금	12:00:00	21:00:00	\N	f	\N
4786	177	목	12:00:00	21:00:00	\N	f	\N
4787	177	수	12:00:00	21:00:00	\N	f	\N
4788	177	월	12:00:00	21:00:00	\N	f	\N
4789	177	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4790	177	토	12:00:00	21:00:00	\N	f	\N
4791	177	화	12:00:00	21:00:00	\N	f	\N
4792	178	금	08:00:00	21:00:00	\N	f	\N
4793	178	목	08:00:00	21:00:00	\N	f	\N
4794	178	수	08:00:00	21:00:00	\N	f	\N
4795	178	월	08:00:00	21:00:00	\N	f	\N
4796	178	일	12:00:00	21:00:00	\N	f	\N
4797	178	토	12:00:00	21:00:00	\N	f	\N
4798	178	화	08:00:00	21:00:00	\N	f	\N
4799	179	금	\N	\N	\N	f	\N
4800	179	목	\N	\N	\N	f	\N
4801	179	수	\N	\N	\N	f	\N
4802	179	월	\N	\N	\N	f	\N
4803	179	일	\N	\N	\N	f	\N
4804	179	토	\N	\N	\N	f	\N
4805	179	화	\N	\N	\N	f	\N
4806	180	금	\N	\N	\N	f	\N
4807	180	목	\N	\N	\N	f	\N
4808	180	수	\N	\N	\N	f	\N
4809	180	월	\N	\N	\N	f	\N
4810	180	일	\N	\N	\N	f	\N
4811	180	토	\N	\N	\N	f	\N
4812	180	화	\N	\N	\N	f	\N
4813	181	금	08:30:00	19:00:00	\N	f	30
4814	181	목	08:30:00	19:00:00	\N	f	30
4815	181	수	08:30:00	19:00:00	\N	f	30
4816	181	월	08:30:00	19:00:00	\N	f	30
4817	181	일	10:00:00	20:00:00	\N	f	30
4818	181	토	10:00:00	20:00:00	\N	f	30
4819	181	화	08:30:00	19:00:00	\N	f	30
4820	182	금	09:00:00	20:30:00	\N	f	60
4821	182	목	09:00:00	20:30:00	\N	f	60
4822	182	수	\N	\N	정기휴무 (매주 수요일)	f	60
4823	182	월	09:00:00	20:30:00	\N	f	60
4824	182	일	11:00:00	19:30:00	\N	f	60
4825	182	토	11:00:00	20:30:00	\N	f	60
4826	182	화	09:00:00	20:30:00	\N	f	60
4827	193	금	09:30:00	20:00:00	\N	f	\N
4828	193	목	09:30:00	20:00:00	\N	f	\N
4829	193	수	09:30:00	20:00:00	\N	f	\N
4830	193	월	09:30:00	20:00:00	\N	f	\N
4831	193	일	09:30:00	22:00:00	\N	f	\N
4832	193	토	09:30:00	22:00:00	\N	f	\N
4833	193	화	11:30:00	14:00:00	\N	f	\N
4834	194	금	\N	\N	\N	f	\N
4835	194	목	\N	\N	\N	f	\N
4836	194	수	\N	\N	\N	f	\N
4837	194	월	\N	\N	\N	f	\N
4838	194	일	\N	\N	\N	f	\N
4839	194	토	\N	\N	\N	f	\N
4840	194	화	\N	\N	\N	f	\N
4841	195	금	\N	\N	\N	f	\N
4842	195	목	\N	\N	\N	f	\N
4843	195	수	\N	\N	\N	f	\N
4844	195	월	\N	\N	\N	f	\N
4845	195	일	\N	\N	\N	f	\N
4846	195	토	\N	\N	\N	f	\N
4847	195	화	\N	\N	\N	f	\N
4848	196	금	10:30:00	22:00:00	\N	f	\N
4849	196	목	10:30:00	22:00:00	\N	f	\N
4850	196	수	10:30:00	22:00:00	\N	f	\N
4851	196	월	10:30:00	22:00:00	\N	f	\N
4852	196	일	11:00:00	19:00:00	\N	f	\N
4853	196	토	10:30:00	22:00:00	\N	f	\N
4854	196	화	10:30:00	22:00:00	\N	f	\N
4855	197	금	08:00:00	19:00:00	\N	f	60
4856	197	목	08:00:00	19:00:00	\N	f	60
4857	197	수	08:00:00	19:00:00	\N	f	60
4858	197	월	08:00:00	19:00:00	\N	f	60
4859	197	일	08:00:00	19:00:00	\N	f	60
4860	197	토	08:00:00	19:00:00	\N	f	60
4861	197	화	08:00:00	19:00:00	\N	f	60
4862	198	금	11:00:00	21:00:00	\N	f	\N
4863	198	목	11:00:00	21:00:00	\N	f	\N
4864	198	수	11:00:00	21:00:00	\N	f	\N
4865	198	월	11:00:00	21:00:00	\N	f	\N
4866	198	일	11:00:00	21:00:00	\N	f	\N
4867	198	토	11:00:00	21:00:00	\N	f	\N
4868	198	화	11:00:00	21:00:00	\N	f	\N
4869	199	금	11:00:00	22:00:00	\N	f	\N
4870	199	목	11:00:00	22:00:00	\N	f	\N
4871	199	수	11:00:00	22:00:00	\N	f	\N
4872	199	월	11:00:00	22:00:00	\N	f	\N
4873	199	일	10:00:00	22:00:00	\N	f	\N
4874	199	토	10:00:00	22:00:00	\N	f	\N
4875	199	화	11:00:00	22:00:00	\N	f	\N
4876	200	금	07:00:00	22:00:00	\N	f	\N
4877	200	목	07:00:00	22:00:00	\N	f	\N
4878	200	수	07:00:00	22:00:00	\N	f	\N
4879	200	월	07:00:00	22:00:00	\N	f	\N
4880	200	일	08:00:00	21:00:00	\N	f	\N
4881	200	토	08:00:00	22:00:00	\N	f	\N
4882	200	화	07:00:00	22:00:00	\N	f	\N
4883	201	금	07:30:00	22:00:00	\N	f	\N
4884	201	목	07:30:00	22:00:00	\N	f	\N
4885	201	수	07:30:00	22:00:00	\N	f	\N
4886	201	월	07:30:00	22:00:00	\N	f	\N
4887	201	일	09:00:00	21:00:00	\N	f	\N
4888	201	토	09:00:00	22:00:00	\N	f	\N
4889	201	화	07:30:00	22:00:00	\N	f	\N
4890	202	금	10:00:00	21:00:00	\N	f	\N
4891	202	목	10:00:00	21:00:00	\N	f	\N
4892	202	수	10:00:00	21:00:00	\N	f	\N
4893	202	월	\N	\N	정기휴무 (매주 월요일)	f	\N
4894	202	일	10:00:00	18:00:00	\N	f	\N
4895	202	토	10:00:00	21:00:00	\N	f	\N
4896	202	화	10:00:00	21:00:00	\N	f	\N
4897	213	금	13:00:00	23:00:00	\N	f	45
4898	213	목	13:00:00	23:00:00	\N	f	45
4899	213	수	13:00:00	23:00:00	\N	f	45
4900	213	월	13:00:00	23:00:00	\N	f	45
4901	213	일	13:00:00	23:00:00	\N	f	45
4902	213	토	13:00:00	23:00:00	\N	f	45
4903	213	화	13:00:00	23:00:00	\N	f	45
4904	214	금	11:00:00	21:30:00	\N	f	420
4905	214	목	11:00:00	21:30:00	\N	f	420
4906	214	수	11:00:00	21:30:00	\N	f	420
4907	214	월	\N	\N	정기휴무 (매주 월요일)	f	420
4908	214	일	11:00:00	21:30:00	\N	f	420
4909	214	토	11:00:00	21:30:00	\N	f	420
4910	214	화	11:00:00	21:30:00	\N	f	420
4911	215	금	06:00:00	\N	\N	f	870
4912	215	목	06:00:00	\N	\N	f	870
4913	215	수	06:00:00	\N	\N	f	870
4914	215	월	11:00:00	\N	\N	f	870
4915	215	일	\N	\N	정기휴무 (매주 일요일)	f	870
4916	215	토	\N	\N	정기휴무 (매주 토요일)	f	870
4917	215	화	06:00:00	\N	\N	f	870
4918	216	금	13:00:00	23:00:00	\N	f	\N
4919	216	목	13:00:00	23:00:00	\N	f	\N
4920	216	수	13:00:00	23:00:00	\N	f	\N
4921	216	월	13:00:00	23:00:00	\N	f	\N
4922	216	일	\N	\N	정기휴무 (매주 일요일)	f	\N
4923	216	토	13:00:00	23:00:00	\N	f	\N
4924	216	화	13:00:00	23:00:00	\N	f	\N
4925	217	금	15:00:00	22:30:00	\N	f	30
4926	217	목	15:00:00	22:30:00	\N	f	30
4927	217	수	15:00:00	22:30:00	\N	f	30
4928	217	월	\N	\N	정기휴무 (매주 월요일)	f	30
4929	217	일	15:00:00	22:30:00	\N	f	30
4930	217	토	15:00:00	22:30:00	\N	f	30
4931	217	화	15:00:00	22:30:00	\N	f	30
4932	218	금	11:30:00	22:00:00	\N	f	\N
4933	218	목	11:30:00	22:00:00	\N	f	\N
4934	218	수	11:30:00	22:00:00	\N	f	\N
4935	218	월	11:30:00	22:00:00	\N	f	\N
4936	218	일	11:30:00	22:00:00	\N	f	\N
4937	218	토	11:30:00	22:00:00	\N	f	\N
4938	218	화	11:30:00	22:00:00	\N	f	\N
4939	219	금	11:30:00	20:00:00	\N	f	300
4940	219	목	11:30:00	20:00:00	\N	f	300
4941	219	수	11:30:00	20:00:00	\N	f	300
4942	219	월	\N	\N	정기휴무 (매주 월요일)	f	300
4943	219	일	11:30:00	15:00:00	\N	f	300
4944	219	토	11:30:00	20:00:00	\N	f	300
4945	219	화	\N	\N	정기휴무 (매주 화요일)	f	300
4946	220	금	11:00:00	22:00:00	\N	f	\N
4947	220	목	11:00:00	22:00:00	\N	f	\N
4948	220	수	11:00:00	22:00:00	\N	f	\N
4949	220	월	11:00:00	22:00:00	\N	f	\N
4950	220	일	11:00:00	22:00:00	\N	f	\N
4951	220	토	11:00:00	22:00:00	\N	f	\N
4952	220	화	11:00:00	22:00:00	\N	f	\N
4953	221	금	11:00:00	22:30:00	\N	f	\N
4954	221	목	11:00:00	22:30:00	\N	f	\N
4955	221	수	11:00:00	22:30:00	\N	f	\N
4956	221	월	11:00:00	22:30:00	\N	f	\N
4957	221	일	11:00:00	22:30:00	\N	f	\N
4958	221	토	11:00:00	22:30:00	\N	f	\N
4959	221	화	11:00:00	22:30:00	\N	f	\N
4960	222	금	11:20:00	20:50:00	\N	f	290
4961	222	목	11:20:00	20:50:00	\N	f	290
4962	222	수	11:20:00	20:50:00	\N	f	290
4963	222	월	11:20:00	20:50:00	\N	f	290
4964	222	일	\N	\N	정기휴무 (매주 일요일)	f	290
4965	222	토	11:20:00	19:00:00	\N	f	290
4966	222	화	11:20:00	20:50:00	\N	f	290
4967	233	금	11:30:00	21:00:00	\N	f	370
4968	233	목	11:30:00	21:00:00	\N	f	370
4969	233	수	11:30:00	21:00:00	\N	f	370
4970	233	월	\N	\N	정기휴무 (매주 월요일)	f	370
4971	233	일	11:30:00	21:00:00	\N	f	370
4972	233	토	11:30:00	21:00:00	\N	f	370
4973	233	화	11:30:00	21:00:00	\N	f	370
4974	234	금	15:00:00	01:00:00	\N	f	\N
4975	234	목	15:00:00	01:00:00	\N	f	\N
4976	234	수	15:00:00	01:00:00	\N	f	\N
4977	234	월	15:00:00	01:00:00	\N	f	\N
4978	234	일	15:00:00	01:00:00	\N	f	\N
4979	234	토	15:00:00	01:00:00	\N	f	\N
4980	234	화	15:00:00	01:00:00	\N	f	\N
4981	235	금	11:30:00	21:00:00	\N	f	330
4982	235	목	11:30:00	21:00:00	\N	f	330
4983	235	수	11:30:00	21:00:00	\N	f	330
4984	235	월	\N	\N	정기휴무 (매주 월요일)	f	330
4985	235	일	11:30:00	21:00:00	\N	f	330
4986	235	토	11:30:00	21:00:00	\N	f	330
4987	235	화	11:30:00	21:00:00	\N	f	330
4988	236	금	11:30:00	19:00:00	\N	f	300
4989	236	목	11:30:00	19:00:00	\N	f	300
4990	236	수	11:30:00	19:00:00	\N	f	300
4991	236	월	11:30:00	19:00:00	\N	f	300
4992	236	일	\N	\N	정기휴무 (매주 일요일)	f	300
4993	236	토	11:30:00	19:00:00	\N	f	300
4994	236	화	11:30:00	19:00:00	\N	f	300
4995	237	금	17:30:00	22:30:00	\N	f	\N
4996	237	목	17:30:00	22:30:00	\N	f	\N
4997	237	수	17:30:00	22:30:00	\N	f	\N
4998	237	월	17:30:00	22:30:00	\N	f	\N
4999	237	일	\N	\N	정기휴무 (매주 일요일)	f	\N
5000	237	토	17:30:00	22:00:00	\N	f	\N
5001	237	화	17:30:00	22:30:00	\N	f	\N
5002	238	금	07:30:00	21:30:00	\N	f	30
5003	238	목	07:30:00	21:30:00	\N	f	30
5004	238	수	07:30:00	21:30:00	\N	f	30
5005	238	월	07:30:00	21:30:00	\N	f	30
5006	238	일	\N	\N	정기휴무 (매주 일요일)	f	30
5007	238	토	08:00:00	19:00:00	\N	f	30
5008	238	화	07:30:00	21:30:00	\N	f	30
5009	239	금	06:30:00	21:30:00	\N	f	660
5010	239	목	06:30:00	21:30:00	\N	f	660
5011	239	수	06:30:00	21:30:00	\N	f	660
5012	239	월	06:30:00	21:30:00	\N	f	660
5013	239	일	06:30:00	21:30:00	\N	f	660
5014	239	토	06:30:00	21:30:00	\N	f	660
5015	239	화	06:30:00	21:30:00	\N	f	660
5016	240	금	11:30:00	21:30:00	\N	f	360
5017	240	목	11:30:00	21:30:00	\N	f	360
5018	240	수	11:30:00	21:30:00	\N	f	360
5019	240	월	11:30:00	21:30:00	\N	f	360
5020	240	일	11:30:00	21:30:00	\N	f	360
5021	240	토	11:30:00	21:30:00	\N	f	360
5022	240	화	11:30:00	21:30:00	\N	f	360
5023	241	금	11:30:00	22:00:00	\N	f	60
5024	241	목	11:30:00	22:00:00	\N	f	60
5025	241	수	11:30:00	22:00:00	\N	f	60
5026	241	월	11:30:00	22:00:00	\N	f	60
5027	241	일	11:30:00	22:00:00	\N	f	60
5028	241	토	11:30:00	22:00:00	\N	f	60
5029	241	화	11:30:00	22:00:00	\N	f	60
5030	242	금	11:00:00	20:30:00	\N	f	360
5031	242	목	11:00:00	20:30:00	\N	f	360
5032	242	수	11:00:00	20:30:00	\N	f	360
5033	242	월	11:00:00	20:30:00	\N	f	360
5034	242	일	11:00:00	20:30:00	\N	f	360
5035	242	토	11:00:00	20:30:00	\N	f	360
5036	242	화	\N	\N	정기휴무 (매주 화요일)	f	360
5037	253	금	11:30:00	21:30:00	\N	f	390
5038	253	목	11:30:00	21:30:00	\N	f	390
5039	253	수	11:30:00	21:30:00	\N	f	390
5040	253	월	11:30:00	21:30:00	\N	f	390
5041	253	일	\N	\N	정기휴무 (매주 일요일)	f	390
5042	253	토	12:00:00	21:30:00	\N	f	390
5043	253	화	11:30:00	21:30:00	\N	f	390
5044	254	금	11:30:00	22:00:00	\N	f	420
5045	254	목	11:30:00	22:00:00	\N	f	420
5046	254	수	11:30:00	22:00:00	\N	f	420
5047	254	월	11:30:00	22:00:00	\N	f	420
5048	254	일	\N	\N	정기휴무 (매주 일요일)	f	420
5049	254	토	12:00:00	21:00:00	\N	f	420
5050	254	화	11:30:00	22:00:00	\N	f	420
5051	255	금	11:30:00	22:00:00	\N	f	60
5052	255	목	11:30:00	22:00:00	\N	f	60
5053	255	수	11:30:00	22:00:00	\N	f	60
5054	255	월	11:30:00	22:00:00	\N	f	60
5055	255	일	12:00:00	21:00:00	\N	f	60
5056	255	토	12:00:00	21:00:00	\N	f	60
5057	255	화	11:30:00	22:00:00	\N	f	60
5058	256	금	09:00:00	22:00:00	\N	f	60
5059	256	목	09:00:00	22:00:00	\N	f	60
5060	256	수	09:00:00	22:00:00	\N	f	60
5061	256	월	09:00:00	22:00:00	\N	f	60
5062	256	일	08:00:00	22:00:00	\N	f	60
5063	256	토	08:00:00	22:00:00	\N	f	60
5064	256	화	09:00:00	22:00:00	\N	f	60
5065	257	금	11:30:00	22:00:00	\N	f	450
5066	257	목	11:30:00	22:00:00	\N	f	450
5067	257	수	11:30:00	22:00:00	\N	f	450
5068	257	월	11:30:00	22:00:00	\N	f	450
5069	257	일	\N	\N	정기휴무 (매주 일요일)	f	450
5070	257	토	\N	\N	정기휴무 (매주 토요일)	f	450
5071	257	화	11:30:00	22:00:00	\N	f	450
5072	258	금	11:00:00	21:00:00	\N	f	30
5073	258	목	11:00:00	21:00:00	\N	f	30
5074	258	수	11:00:00	21:00:00	\N	f	30
5075	258	월	11:00:00	21:00:00	\N	f	30
5076	258	일	11:00:00	19:00:00	\N	f	30
5077	258	토	11:00:00	19:00:00	\N	f	30
5078	258	화	11:00:00	21:00:00	\N	f	30
5079	259	금	10:00:00	20:30:00	\N	f	30
5080	259	목	10:00:00	20:30:00	\N	f	30
5081	259	수	10:00:00	20:30:00	\N	f	30
5082	259	월	10:00:00	20:30:00	\N	f	30
5083	259	일	10:00:00	20:30:00	\N	f	30
5084	259	토	\N	\N	정기휴무 (매주 토요일)	f	30
5085	259	화	10:00:00	20:30:00	\N	f	30
5086	260	금	11:30:00	23:00:00	\N	f	60
5087	260	목	11:30:00	23:00:00	\N	f	60
5088	260	수	11:30:00	23:00:00	\N	f	60
5089	260	월	11:30:00	23:00:00	\N	f	60
5090	260	일	11:30:00	22:30:00	\N	f	60
5091	260	토	11:30:00	22:30:00	\N	f	60
5092	260	화	\N	\N	정기휴무 (매주 화요일)	f	60
5093	261	금	11:00:00	20:00:00	\N	f	\N
5094	261	목	11:00:00	20:00:00	\N	f	\N
5095	261	수	11:00:00	20:00:00	\N	f	\N
5096	261	월	\N	\N	정기휴무 (매주 월요일)	f	\N
5097	261	일	11:00:00	20:00:00	\N	f	\N
5098	261	토	11:00:00	20:00:00	\N	f	\N
5099	261	화	11:00:00	20:00:00	\N	f	\N
5100	262	금	11:30:00	22:00:00	\N	f	\N
5101	262	목	11:30:00	22:00:00	\N	f	\N
5102	262	수	11:30:00	22:00:00	\N	f	\N
5103	262	월	11:30:00	22:00:00	\N	f	\N
5104	262	일	11:30:00	22:00:00	\N	f	\N
5105	262	토	11:30:00	22:00:00	\N	f	\N
5106	262	화	11:30:00	22:00:00	\N	f	\N
5107	273	금	11:30:00	22:00:00	\N	f	420
5108	273	목	11:30:00	22:00:00	\N	f	420
5109	273	수	11:30:00	22:00:00	\N	f	420
5110	273	월	11:30:00	22:00:00	\N	f	420
5111	273	일	11:30:00	22:00:00	\N	f	420
5112	273	토	11:30:00	22:00:00	\N	f	420
5113	273	화	11:30:00	22:00:00	\N	f	420
5114	274	금	11:30:00	22:30:00	\N	f	\N
5115	274	목	11:30:00	22:30:00	\N	f	\N
5116	274	수	11:30:00	22:30:00	\N	f	\N
5117	274	월	11:30:00	22:30:00	\N	f	\N
5118	274	일	\N	\N	\N	f	\N
5119	274	토	\N	\N	\N	f	\N
5120	274	화	11:30:00	22:30:00	\N	f	\N
5121	275	금	11:00:00	18:00:00	\N	f	\N
5122	275	목	11:00:00	18:00:00	\N	f	\N
5123	275	수	11:00:00	18:00:00	\N	f	\N
5124	275	월	\N	\N	정기휴무 (매주 월요일)	f	\N
5125	275	일	11:00:00	18:00:00	\N	f	\N
5126	275	토	11:00:00	20:00:00	\N	f	\N
5127	275	화	11:00:00	18:00:00	\N	f	\N
5128	276	금	10:00:00	18:00:00	\N	f	\N
5129	276	목	10:00:00	18:00:00	\N	f	\N
5130	276	수	10:00:00	18:00:00	\N	f	\N
5131	276	월	\N	\N	정기휴무 (매주 월요일)	f	\N
5132	276	일	10:00:00	18:00:00	\N	f	\N
5133	276	토	10:00:00	18:00:00	\N	f	\N
5134	276	화	10:00:00	18:00:00	\N	f	\N
5135	277	금	11:00:00	22:00:00	\N	f	450
5136	277	목	11:00:00	22:00:00	\N	f	450
5137	277	수	11:00:00	22:00:00	\N	f	450
5138	277	월	11:00:00	22:00:00	\N	f	450
5139	277	일	11:00:00	21:00:00	\N	f	450
5140	277	토	11:00:00	21:00:00	\N	f	450
5141	277	화	11:00:00	22:00:00	\N	f	450
5142	278	금	11:30:00	20:40:00	\N	f	\N
5143	278	목	11:30:00	20:40:00	\N	f	\N
5144	278	수	11:30:00	20:40:00	\N	f	\N
5145	278	월	11:30:00	20:40:00	\N	f	\N
5146	278	일	11:30:00	20:40:00	\N	f	\N
5147	278	토	11:30:00	20:40:00	\N	f	\N
5148	278	화	11:30:00	20:40:00	\N	f	\N
5149	279	금	11:00:00	23:00:00	\N	f	\N
5150	279	목	11:00:00	23:00:00	\N	f	\N
5151	279	수	11:00:00	23:00:00	\N	f	\N
5152	279	월	11:00:00	23:00:00	\N	f	\N
5153	279	일	11:00:00	23:00:00	\N	f	\N
5154	279	토	11:00:00	23:00:00	\N	f	\N
5155	279	화	11:00:00	23:00:00	\N	f	\N
5156	280	금	11:00:00	22:00:00	\N	f	60
5157	280	목	11:00:00	22:00:00	\N	f	60
5158	280	수	11:00:00	22:00:00	\N	f	60
5159	280	월	11:00:00	22:00:00	\N	f	60
5160	280	일	11:00:00	22:00:00	\N	f	60
5161	280	토	11:00:00	22:00:00	\N	f	60
5162	280	화	11:00:00	22:00:00	\N	f	60
5163	281	금	11:00:00	22:00:00	\N	f	\N
5164	281	목	11:00:00	22:00:00	\N	f	\N
5165	281	수	11:00:00	22:00:00	\N	f	\N
5166	281	월	11:00:00	22:00:00	\N	f	\N
5167	281	일	11:00:00	22:00:00	\N	f	\N
5168	281	토	11:00:00	22:00:00	\N	f	\N
5169	281	화	11:00:00	22:00:00	\N	f	\N
5170	282	금	11:30:00	23:00:00	\N	f	\N
5171	282	목	11:30:00	23:00:00	\N	f	\N
5172	282	수	11:30:00	23:00:00	\N	f	\N
5173	282	월	11:30:00	23:00:00	\N	f	\N
5174	282	일	\N	\N	정기휴무 (매주 일요일)	f	\N
5175	282	토	11:30:00	23:00:00	\N	f	\N
5176	282	화	11:30:00	23:00:00	\N	f	\N
\.


--
-- Data for Name: place_description_vectors; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_description_vectors (id, combined_attributes_text, created_at, description_vector, extraction_prompt_hash, extraction_source, model_name, model_version, raw_description_text, selected_keywords, updated_at, place_id) FROM stdin;
\.


--
-- Data for Name: place_descriptions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_descriptions (id, place_id, original_description, ai_summary, ollama_description, search_query, updated_at) FROM stdin;
\.


--
-- Data for Name: place_external_raw; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_external_raw (id, external_id, fetched_at, payload, place_id, source) FROM stdin;
\.


--
-- Data for Name: place_images; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_images (id, place_id, url, order_index, created_at) FROM stdin;
\.


--
-- Data for Name: place_mbti_descriptions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_mbti_descriptions (id, description, mbti, model, place_id, prompt_hash, updated_at) FROM stdin;
\.


--
-- Data for Name: place_reviews; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_reviews (id, place_id, review_text, order_index, created_at) FROM stdin;
\.


--
-- Data for Name: place_similarity; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_similarity (place_id1, place_id2, co_users, cosine_bin, jaccard, updated_at) FROM stdin;
\.


--
-- Data for Name: place_similarity_topk; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_similarity_topk (neighbor_place_id, place_id, co_users, cosine_bin, jaccard, rank, updated_at) FROM stdin;
\.


--
-- Data for Name: place_sns; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_sns (id, place_id, platform, url, created_at) FROM stdin;
\.


--
-- Data for Name: places; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.places (id, name, latitude, longitude, road_address, website_url, rating, review_count, category, keyword, keyword_vector, opening_hours, parking_available, pet_friendly, ready, created_at, updated_at, crawler_found) FROM stdin;
\.


--
-- Data for Name: preferences; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.preferences (id, created_at, pref_key, pref_value, user_id) FROM stdin;
\.


--
-- Data for Name: prompts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.prompts (id, content, created_at, place_id, user_id) FROM stdin;
\.


--
-- Data for Name: recent_views; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recent_views (id, viewed_at, place_id, user_id) FROM stdin;
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_tokens (id, user_id, token, expiry_date, created_at, expires_at, is_revoked) FROM stdin;
\.


--
-- Data for Name: temp_users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.temp_users (id, email, otp, expiry_date, verified, created_at, expires_at, nickname, password_hash, terms_agreed, verification_code) FROM stdin;
\.


--
-- Data for Name: terms_agreements; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.terms_agreements (id, agreed, agreed_at, terms_code, user_id) FROM stdin;
\.


--
-- Data for Name: user_preference_vectors; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_preference_vectors (id, combined_preferences_text, created_at, extraction_prompt_hash, extraction_source, model_name, model_version, preference_vector, raw_profile_text, selected_keywords, updated_at, user_id) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, email, password, nickname, mbti, age_range, transportation_method, space_preferences, created_at, updated_at, is_onboarding_completed, last_login_at, password_hash, profile_image_url, transportation) FROM stdin;
\.


--
-- Data for Name: vector_similarities; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vector_similarities (place_id, user_id, calculated_at, common_keywords, cosine_similarity, euclidean_distance, jaccard_similarity, keyword_overlap_ratio, mbti_boost_factor, place_vector_version, user_vector_version, weighted_similarity) FROM stdin;
\.


--
-- Name: activities_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.activities_id_seq', 1, false);


--
-- Name: batch_job_execution_job_execution_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_job_execution_job_execution_id_seq', 1, false);


--
-- Name: batch_job_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_job_execution_seq', 1, false);


--
-- Name: batch_job_instance_job_instance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_job_instance_job_instance_id_seq', 1, false);


--
-- Name: batch_job_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_job_seq', 1, false);


--
-- Name: batch_step_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_step_execution_seq', 1, false);


--
-- Name: batch_step_execution_step_execution_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_step_execution_step_execution_id_seq', 1, false);


--
-- Name: bookmarks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.bookmarks_id_seq', 1, false);


--
-- Name: email_verifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.email_verifications_id_seq', 1, false);


--
-- Name: keyword_catalog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.keyword_catalog_id_seq', 1, false);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.password_reset_tokens_id_seq', 1, false);


--
-- Name: place_business_hours_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_business_hours_id_seq', 1, false);


--
-- Name: place_description_vectors_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_description_vectors_id_seq', 1, false);


--
-- Name: place_descriptions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_descriptions_id_seq', 1, false);


--
-- Name: place_external_raw_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_external_raw_id_seq', 1, false);


--
-- Name: place_images_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_images_id_seq', 1, false);


--
-- Name: place_mbti_descriptions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_mbti_descriptions_id_seq', 1, false);


--
-- Name: place_reviews_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_reviews_id_seq', 1, false);


--
-- Name: place_sns_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_sns_id_seq', 1, false);


--
-- Name: places_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.places_id_seq', 1, false);


--
-- Name: preferences_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.preferences_id_seq', 1, false);


--
-- Name: prompts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.prompts_id_seq', 1, false);


--
-- Name: recent_views_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recent_views_id_seq', 1, false);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.refresh_tokens_id_seq', 1, false);


--
-- Name: temp_users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.temp_users_id_seq', 1, false);


--
-- Name: terms_agreements_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.terms_agreements_id_seq', 1, false);


--
-- Name: user_preference_vectors_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_preference_vectors_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 1, false);


--
-- PostgreSQL database dump complete
--

\unrestrict lWDrOvKsd7aqrQQFJkpEr6kyLab7ZWe4VSxYWhKOyxQRLsdK8tYkwFWnGxwgbRB

